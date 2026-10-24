package com.yjw.vultr;
import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import android.util.Base64;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;
final class Vault {
 private final AtomicFile file;
 private final javax.crypto.SecretKey key;
 Vault(Context c)throws Exception{
  file=new AtomicFile(new File(c.getFilesDir(),"vault.enc"));
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias("vultr-pocket")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("vultr-pocket",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
  key=(javax.crypto.SecretKey)ks.getKey("vultr-pocket",null);
 }
 synchronized JSONObject read()throws Exception{
  if(!file.getBaseFile().exists())return new JSONObject().put("records",new JSONArray()).put("region","sjc").put("plan","vc2-1c-1gb").put("cap","5");
  JSONObject x=new JSONObject(new String(file.readFully(),"UTF-8"));Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(x.getString("iv"),0)));
  return new JSONObject(new String(c.doFinal(Base64.decode(x.getString("data"),0)),"UTF-8"));
 }
 synchronized void write(JSONObject data)throws Exception{
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key);
  byte[] out=new JSONObject().put("iv",Base64.encodeToString(c.getIV(),2)).put("data",Base64.encodeToString(c.doFinal(data.toString().getBytes("UTF-8")),2)).toString().getBytes("UTF-8");
  FileOutputStream stream=null;try{stream=file.startWrite();stream.write(out);file.finishWrite(stream);}catch(Exception e){if(stream!=null)file.failWrite(stream);throw e;}
 }
}
