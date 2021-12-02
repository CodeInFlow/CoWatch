package com.codeinflow.cowatch;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Login extends Fragment {

    EditText name,mobile,otp,email;
    CheckBox cb;
    Button btn_getOtp;

    BottomSheetDialog bottomSheetDialog;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String mVerificationId;
    Map<String, Object> data = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        View v1 = inflater.inflate(R.layout.otp_sheet,container,false);

        name = v.findViewById(R.id.login_name);
        mobile = v.findViewById(R.id.login_phn);
        otp = v1.findViewById(R.id.et_otp);
        email= v.findViewById(R.id.login_email);
        cb = v.findViewById(R.id.login_checkbox);
        btn_getOtp = v.findViewById(R.id.btn_getOTP );

        btn_getOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mobile.getText().toString().length() != 10) {
                    mobile.setError("Enter a valid mobile");
                    mobile.requestFocus();
                }
                if (name.getText().toString().isEmpty()) {
                    name.setError("Enter a valid Name");
                    name.requestFocus();
                }
                if (email.getText().toString().isEmpty()) {
                    email.setError("Enter a valid Email Address");
                    email.requestFocus();
                } else {
                    data.put("name", name.getText().toString());
                    data.put("number", mobile.getText().toString());
                    data.put("email", email.getText().toString());
                    if (cb.isChecked())
                        data.put("Newsletter", "Yes");
                    else
                        data.put("Newsletter", "No");
                    showBottomSheetDialog();
                }
            }
        });

        return v;
    }

    public void showBottomSheetDialog() {

        sendVerificationCode(mobile.getText().toString());
        bottomSheetDialog = new BottomSheetDialog(getActivity());
        bottomSheetDialog.setContentView(R.layout.otp_sheet);
        bottomSheetDialog.show();
        bottomSheetDialog.setCancelable(false);
        bottomSheetDialog.setCanceledOnTouchOutside(false);

        Button login = bottomSheetDialog.findViewById(R.id.btn_login);
        final EditText otp = bottomSheetDialog.findViewById(R.id.et_otp);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String code = otp.getText().toString().trim();
                if (code.isEmpty() || code.length() < 6) {
                    otp.setError("Enter valid code");
                    otp.requestFocus();
                    return;
                } else {
                    verifyVerificationCode(code);

                }
            }

        });
    }

    private void sendVerificationCode(String no) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91" + no,
                60,
                TimeUnit.SECONDS,
                getActivity(),
                mCallbacks);
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

            String code = phoneAuthCredential.getSmsCode();

            if (code != null) {
                verifyVerificationCode(code);
            }
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            bottomSheetDialog.dismiss();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);

            mVerificationId = s;
        }
    };

    private void verifyVerificationCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            addDataToFirestore();

                        } else {


                            String message = "Somthing is wrong, we will fix it soon...";

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid code entered...";

                            }
                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT);
                            bottomSheetDialog.dismiss();


                        }
                    }
                });
    }


    private void addDataToFirestore() {

        db = FirebaseFirestore.getInstance();
        String currentuser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference dbCourses = db.collection("Users").document(currentuser);

        dbCourses.set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intent = new Intent(getActivity(), MainActivity2.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

    }
}