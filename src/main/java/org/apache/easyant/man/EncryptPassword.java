package org.apache.easyant.man;


public class EncryptPassword extends EasyantOption {
    
    public EncryptPassword() {
        super("encrypt",true,"encrypt password");
        setStopBuild(true);
    }

    @Override
    public void execute() {
        getProject().log("encryptedpassword for " + getValue());
    }

}
