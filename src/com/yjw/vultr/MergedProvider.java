package com.yjw.vultr;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
public class MergedProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 File file(Uri uri)throws FileNotFoundException{String name=uri.getLastPathSegment();if(name==null||!name.matches("merged-[a-f0-9-]+\\.yaml"))throw new FileNotFoundException();File f=new File(new File(getContext().getFilesDir(),"exports"),name);if(!f.isFile())throw new FileNotFoundException();return f;}
 public String getType(Uri uri){return "application/yaml";}
 public Cursor query(Uri uri,String[] projection,String sel,String[] args,String sort){try{File f=file(uri);MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{"Vultr-合并节点.yaml",f.length()});return c;}catch(Exception e){return null;}}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
