package nl.tudelft.cs4160.trustchain_android.funds.qr;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.google.zxing.Result;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.crypto.DualSecret;
import nl.tudelft.cs4160.trustchain_android.crypto.Key;
import nl.tudelft.cs4160.trustchain_android.funds.qr.exception.QRWalletImportException;
import nl.tudelft.cs4160.trustchain_android.funds.qr.exception.QRWalletParseException;
import nl.tudelft.cs4160.trustchain_android.funds.qr.exception.QRWalletValidationException;
import nl.tudelft.cs4160.trustchain_android.funds.qr.models.QRTransaction;
import nl.tudelft.cs4160.trustchain_android.funds.qr.models.QRWallet;
import nl.tudelft.cs4160.trustchain_android.message.MessageProto;
import nl.tudelft.cs4160.trustchain_android.storage.database.AppDatabase;
import nl.tudelft.cs4160.trustchain_android.storage.repository.BlockRepository;
import nl.tudelft.cs4160.trustchain_android.util.Util;


public class ScanQRActivity extends AppCompatActivity {
    public static final int PERMISSIONS_REQUEST_CAMERA = 0;
    public static final String TAG = "ScanQRActivity";

    private Vibrator vibrator;
    private ZXingScannerView scannerView;

    private TrustChainBlockFactory trustChainQRBlockFactory = new TrustChainBlockFactory();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        scannerView = findViewById(R.id.scanner_view);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Camera request permission handling
        if (hasCameraPermission()) {
            startCamera();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

                DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        requestCameraPermission();
                    }
                };

                new AlertDialog.Builder(this).setTitle(R.string.camera_permissions_required)
                        .setMessage(R.string.camera_permisions_required_long)
                        .setNeutralButton(android.R.string.ok, null)
                        .setOnDismissListener(dismissListener)
                        .show();
            } else {
                requestCameraPermission();
            }
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                PERMISSIONS_REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                finish();
            }
        }
    }

    private ZXingScannerView.ResultHandler scanResultHandler = new ZXingScannerView.ResultHandler() {
        public void handleResult(Result result) {
            vibrator.vibrate(100);
            try {
                QRWallet wallet = processResult(result);
                QRTransaction transaction = wallet.transaction;

                String message = "Successful transaction"
                        + "\nUp=" + Util.readableSize(transaction.up)
                        + "\nTotalUp=" + Util.readableSize(transaction.totalUp)
                        + "\nDown=" + Util.readableSize(transaction.down)
                        + "\nTotalDown=" + Util.readableSize(transaction.totalDown) ;

                new AlertDialog.Builder(ScanQRActivity.this)
                        .setTitle("Success")
                        .setMessage(message)
                        .setNeutralButton(android.R.string.ok, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                ScanQRActivity.this.finish();
                            }
                        }).show();

            } catch (QRWalletImportException exception) {
                Log.e(TAG, "Could not import QR Wallet", exception);
                new AlertDialog.Builder(ScanQRActivity.this)
                        .setTitle("Error")
                        .setMessage("Something went wrong processing the QR data")
                        .setNeutralButton(android.R.string.ok, null)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                scannerView.resumeCameraPreview(scanResultHandler);
                            }
                        }).show();
            }
        }
    };

    private void startCamera() {
        scannerView.setResultHandler(scanResultHandler);
        scannerView.startCamera();
    }

    private QRWallet processResult(Result result) throws QRWalletImportException {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<QRWallet> walletAdapter = moshi.adapter(QRWallet.class);

        QRWallet wallet;
        try {
            wallet = walletAdapter.fromJson(result.getText());
        } catch (IOException e) {
            throw new QRWalletParseException(e);
        }
        if (wallet == null) {
            throw new QRWalletParseException("Null wallet");
        }

        DualSecret ownKeyPair = Key.loadKeys(this);
        BlockRepository blockRepository = new BlockRepository(AppDatabase.getInstance(this).blockDao());
        MessageProto.TrustChainBlock block = trustChainQRBlockFactory.createBlock(wallet, blockRepository, ownKeyPair);

        try {
//            TrustChainBlock.validate(block, helper);
            MessageProto.TrustChainBlock halfblock = trustChainQRBlockFactory.reconstructTemporaryIdentityHalfBlock(wallet);

            blockRepository.insertOrUpdate(halfblock);
            blockRepository.insertOrUpdate(block);
        } catch (Exception e) {
            throw new QRWalletValidationException(e);
        }

        return wallet;
    }

    private boolean hasCameraPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }
}

