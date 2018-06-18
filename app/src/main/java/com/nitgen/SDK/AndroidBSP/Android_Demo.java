/*
 * COPYRIGHT(c) UNIONCOMMUNITY 2013
 * This software is the proprietary information of UNIONCOMMUNITY
 *
 */
package com.nitgen.SDK.AndroidBSP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.app.Activity;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nitgen.SDK.AndroidBSP.NBioBSPJNI.CAPTURED_DATA;
import com.nitgen.SDK.AndroidBSP.NBioBSPJNI.CAPTURE_CALLBACK;
import com.nitgen.SDK.AndroidBSP.NBioBSPJNI.CAPTURE_QUALITY_INFO;
import com.nitgen.SDK.AndroidBSP.NBioBSPJNI.INIT_INFO_0;
import com.nitgen.SDK.AndroidBSP.NBioBSPJNI.NFIQInfo;
import com.nitgen.SDK.AndroidBSP.NBioBSPJNI.IndexSearch.FP_INFO;
import com.nitgen.SDK.AndroidBSP.NBioBSPJNI.IndexSearch.SAMPLE_INFO;
import com.nitgen.SDK.AndroidBSP.SampleDialogFragment.SampleDialogListener;
import com.nitgen.SDK.AndroidBSP.UserDialog.UserDialogListener;

/**
 * Demo App
 * com.nitgen.SDK.AndroidBSP
 * Android_Demo.java
 *
 *@author : KimDoHyun ( rkwkgo@unioncomm.co.kr ) 
 *@since : 2013. 7. 26.
 *update history 
 *-------------------------------------------------
 *@editor : 
 *@edit date : 
 *@edit content :
 *-------------------------------------------------
 */
public class Android_Demo extends Activity implements SampleDialogListener, UserDialogListener{
	
	private static final String TAG = Android_Demo.class.getSimpleName();
	
	private NBioBSPJNI				bsp;
	private NBioBSPJNI.Export       exportEngine;
	private NBioBSPJNI.IndexSearch  indexSearch;
	private byte[]					byTemplate1;
	private byte[]					byTemplate2;
	
	private byte[]					byCapturedRaw1;
	private int						nCapturedRawWidth1;
	private int						nCapturedRawHeight1;

	private byte[]					byCapturedRaw2;
	private int						nCapturedRawWidth2;
	private int						nCapturedRawHeight2;
	
	ImageView img_fp_src, img_fp_dst; 
	TextView tvInfo, tvVer, tvDevice;	
	EditText editBrightness;
	Button btnCapture1, btnCapture2, btnVerifyTemplate, btnVerifyRaw, btnAutoOn1, btnAutoOn2, btnSetBrightness;
	
	DialogFragment sampleDialogFragment;
	UserDialog userDialog;
	
	private boolean					bCapturedFirst, bAutoOn = false;
	
	public static final int QUALITY_LIMIT = 60;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initView();
        initData();
        
    }
    
    /**
     * void
     */
    public void initView(){
    
    	setContentView(R.layout.activity_android__demo);
    	
    	img_fp_src = (ImageView)findViewById(R.id.img_fp_src);
    	img_fp_src.setBackgroundColor(Color.argb(255, 255, 255, 255));
    	img_fp_dst = (ImageView)findViewById(R.id.img_fp_dst);
    	img_fp_dst.setBackgroundColor(Color.argb(255, 255, 255, 255));
    	    	
    	tvInfo = (TextView) findViewById(R.id.textInfo);
    	tvVer = (TextView) findViewById(R.id.textVer);
    	tvDevice = (TextView) findViewById(R.id.textDevice);
    	
    	editBrightness = (EditText)findViewById(R.id.editBrightness);
    	
        btnCapture1 = (Button) findViewById(R.id.btnCapture1);
    	btnCapture1.setEnabled(false);
    	btnAutoOn1 = (Button) findViewById(R.id.btnAutoOn1);
    	btnAutoOn1.setEnabled(false);
    	btnCapture2 = (Button) findViewById(R.id.btnCapture2);
    	btnCapture2.setEnabled(false);
    	btnAutoOn2 = (Button) findViewById(R.id.btnAutoOn2);
    	btnAutoOn2.setEnabled(false);
    	btnVerifyTemplate = (Button) findViewById(R.id.btnVerifyIso);
    	btnVerifyTemplate.setEnabled(false);
    	btnVerifyRaw = (Button) findViewById(R.id.btnVerifyRaw);
    	btnVerifyRaw.setEnabled(false);
    	btnSetBrightness = (Button) findViewById(R.id.btnSetBrightness);
    	btnSetBrightness.setEnabled(false);
    
    }

    /**
     * void
     */
    public void initData(){

    	NBioBSPJNI.CURRENT_PRODUCT_ID = 0;
    	if(bsp==null){
    		bsp = new NBioBSPJNI("010701-613E5C7F4CC7C4B0-72E340B47E034015", this,  mCallback);
    		String msg = null;
    		if (bsp.IsErrorOccured())
    			msg = "NBioBSP Error: " + bsp.GetErrorCode();
    		else  {
    			msg = "SDK Version: " + bsp.GetVersion();
    			exportEngine = bsp.new Export();
    			indexSearch = bsp.new IndexSearch();
    		}
    		tvVer.setText(msg);
    	}

    	sampleDialogFragment = new SampleDialogFragment();
    	userDialog = new UserDialog();
    }

    
    @Override
    public void onDestroy(){
    	
    	if (bsp != null) {
    		bsp.dispose();
    		bsp = null;
    	}
    	super.onDestroy();
   
    }
    
    CAPTURE_CALLBACK mCallback = new CAPTURE_CALLBACK() {
    	
		public void OnDisConnected() {
			NBioBSPJNI.CURRENT_PRODUCT_ID = 0;
			
			if(sampleDialogFragment!=null)
				sampleDialogFragment.dismiss();
			
			String message = "NBioBSP Disconnected: " + bsp.GetErrorCode();
			tvDevice.setText(message);
			
			btnCapture1.setEnabled(false);
			btnSetBrightness.setEnabled(false);
			btnCapture2.setEnabled(false);
			btnAutoOn1.setEnabled(false);
			btnAutoOn2.setEnabled(false);
			btnVerifyTemplate.setEnabled(false);
	        btnVerifyRaw.setEnabled(false);
			
		}
		
		public void OnConnected() {
			if(sampleDialogFragment!=null)
				sampleDialogFragment.dismiss();
			
			String message = "Device Open Success : ";
			
			ByteBuffer deviceId = ByteBuffer.allocate(StaticVals.wLength_GET_ID);
			deviceId.order(ByteOrder.BIG_ENDIAN);
	        bsp.getDeviceID(deviceId.array());
	        
	        if (bsp.IsErrorOccured())  {
	        	msg = "NBioBSP GetDeviceID Error: " + bsp.GetErrorCode();
	        	tvDevice.setText(msg);
	        	return;
	        }

	        ByteBuffer setValue = ByteBuffer.allocate(StaticVals.wLength_SET_VALUE);
	        setValue.order(ByteOrder.BIG_ENDIAN);
	        
	        byte[] src = new byte[StaticVals.wLength_SET_VALUE];
	        for(int i=0;i<src.length;i++){
	        	src[i] = 1;
	        }
	        setValue.put(src);
	        bsp.setValue(setValue.array());
	        
	        if (bsp.IsErrorOccured())  {
	        	msg = "NBioBSP SetValue Error: " + bsp.GetErrorCode();
	        	tvDevice.setText(msg);
	        	return;
	        }
	        
	        ByteBuffer getvalue = ByteBuffer.allocate(StaticVals.wLength_GET_VALUE);
	        getvalue.order(ByteOrder.BIG_ENDIAN);
	        bsp.getValue(getvalue.array());
	        
	        if (bsp.IsErrorOccured())  {
	        	msg = "NBioBSP GetValue Error: " + bsp.GetErrorCode();
	        	tvDevice.setText(msg);
	        	return;
	        }
	        src = new byte[StaticVals.wLength_SET_VALUE];
	        System.arraycopy(getvalue.array(), 0, src, 0, StaticVals.wLength_GET_VALUE);
//	        message += " \n";
//	        for(int i=0;i<src.length;i++){	        	
//	        	message += src[i];
//	        }

			INIT_INFO_0 init_info_0 = bsp.new INIT_INFO_0();
			bsp.GetInitInfo(init_info_0);
			
			CAPTURE_QUALITY_INFO mCAPTURE_QUALITY_INFO = bsp.new CAPTURE_QUALITY_INFO();
			bsp.GetCaptureQualityInfo(mCAPTURE_QUALITY_INFO);
						
			mCAPTURE_QUALITY_INFO.EnrollCoreQuality = 70;
			mCAPTURE_QUALITY_INFO.EnrollFeatureQuality = 30;
			mCAPTURE_QUALITY_INFO.VerifyCoreQuality = 70;
			mCAPTURE_QUALITY_INFO.VerifyFeatureQuality = 30;
			bsp.SetCaptureQualityInfo(mCAPTURE_QUALITY_INFO);
			

//			message = message +":"+init_info_0.EnrollImageQuality;
			
			tvDevice.setText(message);
			
			btnCapture1.setEnabled(true);
			btnSetBrightness.setEnabled(true);
			btnCapture2.setEnabled(true);
			btnAutoOn1.setEnabled(true);
			btnAutoOn2.setEnabled(true);
	        btnVerifyTemplate.setEnabled(false);
	        btnVerifyRaw.setEnabled(false);
			
		}
		
		public int OnCaptured(CAPTURED_DATA capturedData) {
	    	tvDevice.setText("IMAGE Quality: "+capturedData.getImageQuality());	
	    	
	    	if( capturedData.getImage()!=null){    		
	    		if (bCapturedFirst){    		
	    			img_fp_src.setImageBitmap( capturedData.getImage());
	    		}else{
	    			img_fp_dst.setImageBitmap( capturedData.getImage());
	    		}
	    	}

	    	// quality : 40~100
	    	if(capturedData.getImageQuality()>=QUALITY_LIMIT){
	    		if(sampleDialogFragment!=null && "DIALOG_TYPE_PROGRESS".equals(sampleDialogFragment.getTag()))
	    			sampleDialogFragment.dismiss();
	    		return NBioBSPJNI.ERROR.NBioAPIERROR_USER_CANCEL;
	    	}else if(capturedData.getDeviceError()!=NBioBSPJNI.ERROR.NBioAPIERROR_NONE){
	    		if(sampleDialogFragment!=null && "DIALOG_TYPE_PROGRESS".equals(sampleDialogFragment.getTag()))
	    			sampleDialogFragment.dismiss();
	    		return capturedData.getDeviceError();
	    	}else{
	    		return NBioBSPJNI.ERROR.NBioAPIERROR_NONE;    		
	    	}
		}
		
	};
    
    
	/* (non-Javadoc)
	 * @see com.nitgen.SDK.AndroidBSP.SampleDialogFragment.SampleDialogListener#onClickStopBtn(android.app.DialogFragment)
	 */
	public void onClickStopBtn(DialogFragment dialogFragment) {

		bAutoOn = false;
		sampleDialogFragment.dismiss();
		bsp.CaptureCancel();

	}

    /**
     * void
     */
    public void OnBtnOpenDevice(View target){

    	sampleDialogFragment.show(getFragmentManager(), "DIALOG_TYPE_PROGRESS");		    	
    	bsp.OpenDevice();

    }
    
    /**
     * void
     */
    public void OnSetBrightness(View view){
        
    	String brightnessStr = editBrightness.getText().toString();
    	if(!"".equals(brightnessStr)){    		
    		bsp.SetBrightness(Integer.parseInt(brightnessStr));
    	}
    	
    }
    
    public void OnBtnAutoOn1(View target){
		
    	sampleDialogFragment.show(getFragmentManager(), "DIALOG_TYPE_STOP");
    	sampleDialogFragment.setCancelable(false);
    	bAutoOn = true;
		new Thread(new Runnable() {
			
			public void run() {

				while(bAutoOn){
					
					byte[] bFingerExist = new byte[1];
					bFingerExist[0] = 0;
					bsp.CheckFinger(bFingerExist);
					
					if(bFingerExist[0]==1){
						OnCapture1(1000);
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					
				}
				
			}
		}).start();
		
    	
    }
    
    public void OnBtnAutoOn2(View target){
    	
    	sampleDialogFragment.show(getFragmentManager(), "DIALOG_TYPE_STOP");
    	sampleDialogFragment.setCancelable(false);
    	bAutoOn = true;
		new Thread(new Runnable() {
			
			public void run() {

				while(bAutoOn){
					
					byte[] bFingerExist = new byte[1];
					bFingerExist[0] = 0;
					bsp.CheckFinger(bFingerExist);
					
					if(bFingerExist[0]==1){
						OnCapture2(1000);
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				
			}
		}).start();
		
    	
    }
    
	/**
	 * void
	 */
	public void OnBtnCapture1(View target) {
		
//		sampleDialogFragment.show(getFragmentManager(), "DIALOG_TYPE_STOP");
//		sampleDialogFragment.setCancelable(false);
//
		new Thread(new Runnable() {
			
			public void run() {

				OnCapture1(10000);
				
			}
		}).start();
				
	}
	
	int nFIQ = 0;
	String msg = "";
	public synchronized void OnCapture1(int timeout){
		
		NBioBSPJNI.FIR_HANDLE hCapturedFIR, hAuditFIR;
    	NBioBSPJNI.CAPTURED_DATA capturedData;
    	
    	hCapturedFIR = bsp.new FIR_HANDLE();
    	hAuditFIR = bsp.new FIR_HANDLE();
    	capturedData = bsp.new CAPTURED_DATA();
    	
    	bCapturedFirst = true;
		
		bsp.Capture(NBioBSPJNI.FIR_PURPOSE.ENROLL,hCapturedFIR,timeout, hAuditFIR, capturedData,  mCallback,0, null);
		
		if(sampleDialogFragment!=null && "DIALOG_TYPE_PROGRESS".equals(sampleDialogFragment.getTag()))
			sampleDialogFragment.dismiss();
		
		if (bsp.IsErrorOccured())  {
        	msg = "NBioBSP Capture Error: " + bsp.GetErrorCode();
        }
        else  {
        	NBioBSPJNI.INPUT_FIR inputFIR;
        	
        	inputFIR = bsp.new INPUT_FIR();
        	
        	// Make ISO 19794-2 data
        	{
        		NBioBSPJNI.Export.DATA exportData;
        		
        		inputFIR.SetFIRHandle(hCapturedFIR);
        		
        		exportData = exportEngine.new DATA();
        		
        		exportEngine.ExportFIR(inputFIR, exportData, NBioBSPJNI.EXPORT_MINCONV_TYPE.OLD_FDA);
        		
        		if (bsp.IsErrorOccured())  {
        			runOnUiThread(new Runnable() {
						
						public void run() {
							msg = "NBioBSP ExportFIR Error: " + bsp.GetErrorCode();
							tvInfo.setText(msg);
							Toast.makeText(Android_Demo.this, msg, Toast.LENGTH_SHORT).show();
						}
					});
            		return ;
            	}
        		
        		if (byTemplate1 != null)
        			byTemplate1 = null;
        		
        		byTemplate1 = new byte[exportData.FingerData[0].Template[0].Data.length];
        		byTemplate1 = exportData.FingerData[0].Template[0].Data;
        	}
        	
        	// Make Raw Image data
        	{
        		NBioBSPJNI.Export.AUDIT exportAudit;
        		
        		inputFIR.SetFIRHandle(hAuditFIR);
        		
        		exportAudit = exportEngine.new AUDIT();
        		
        		exportEngine.ExportAudit(inputFIR, exportAudit);
        		
        		if (bsp.IsErrorOccured())  {
        			
        			runOnUiThread(new Runnable() {
						
						public void run() {
							msg = "NBioBSP ExportAudit Error: " + bsp.GetErrorCode();
							tvInfo.setText(msg);
							Toast.makeText(Android_Demo.this, msg, Toast.LENGTH_SHORT).show();
						}
					});
        			
            		return ;
            	}
        		
        		if (byCapturedRaw1 != null)
        			byCapturedRaw1 = null;
        		
        		byCapturedRaw1 = new byte[exportAudit.FingerData[0].Template[0].Data.length];
        		byCapturedRaw1 = exportAudit.FingerData[0].Template[0].Data;
    			
    			nCapturedRawWidth1 = exportAudit.ImageWidth;
    			nCapturedRawHeight1 = exportAudit.ImageHeight;
    			
				msg = "First Capture Success";
				
				NFIQInfo info = bsp.new NFIQInfo();				
				info.pRawImage = byCapturedRaw1; 
				info.nImgWidth = nCapturedRawWidth1;
				info.nImgHeight = nCapturedRawHeight1;
				
				bsp.getNFIQInfoFromRaw(info);
				
				if (bsp.IsErrorOccured())  {
					runOnUiThread(new Runnable() {
						
						public void run() {
							msg = "NBioBSP getNFIQInfoFromRaw Error: " + bsp.GetErrorCode();
							tvInfo.setText(msg);
						}
					});
        			
            		return ;
				}

				nFIQ = info.pNFIQ;
				
        	}

        }
		
		runOnUiThread(new Runnable() {
			
			public void run() {
				tvInfo.setText(msg+",NFIQ:"+nFIQ);
				
				if (byTemplate1 != null && byTemplate2 != null)  {
					btnVerifyTemplate.setEnabled(true);
				}else{
					btnVerifyTemplate.setEnabled(false);
				}
				
				if (byCapturedRaw1 != null && byCapturedRaw2 != null)  {
					btnVerifyRaw.setEnabled(true);
				}else{
					btnVerifyRaw.setEnabled(false);
				}
				
			}
		});
		
	}
	
	public synchronized void OnCapture2(int timeout){
		
		
		NBioBSPJNI.FIR_HANDLE hCapturedFIR, hAuditFIR;
    	NBioBSPJNI.CAPTURED_DATA capturedData;
    	hCapturedFIR = bsp.new FIR_HANDLE();
    	hAuditFIR = bsp.new FIR_HANDLE();
    	capturedData = bsp.new CAPTURED_DATA();
    	bCapturedFirst = false;
		
    	bsp.Capture(NBioBSPJNI.FIR_PURPOSE.ENROLL,hCapturedFIR,timeout, hAuditFIR, capturedData,  mCallback,0,null);
		
		if (bsp.IsErrorOccured())  {
        	msg = "NBioBSP Capture Error: " + bsp.GetErrorCode();
        }
        else  {
        	NBioBSPJNI.INPUT_FIR inputFIR;
        	
        	inputFIR = bsp.new INPUT_FIR();
        	
        	// Make ISO 19794-2 data
        	{
        		NBioBSPJNI.Export.DATA exportData;
        		
        		inputFIR.SetFIRHandle(hCapturedFIR);
        		
        		exportData = exportEngine.new DATA();
        		
        		exportEngine.ExportFIR(inputFIR, exportData, NBioBSPJNI.EXPORT_MINCONV_TYPE.OLD_FDA);
        		
        		if (bsp.IsErrorOccured())  {
        			runOnUiThread(new Runnable() {
						
						public void run() {
							msg = "NBioBSP ExportFIR Error: " + bsp.GetErrorCode();
							tvInfo.setText(msg);
							Toast.makeText(Android_Demo.this, msg, Toast.LENGTH_SHORT).show();
						}
					});
            		return ;
            	}
        		
        		if (byTemplate2 != null)
        			byTemplate2 = null;
        		
        		byTemplate2 = new byte[exportData.FingerData[0].Template[0].Data.length];
        		byTemplate2 = exportData.FingerData[0].Template[0].Data;
        	}
        	
        	// Make Raw Image data
        	{
        		NBioBSPJNI.Export.AUDIT exportAudit;
        		
        		inputFIR.SetFIRHandle(hAuditFIR);
        		
        		exportAudit = exportEngine.new AUDIT();
        		
        		exportEngine.ExportAudit(inputFIR, exportAudit);
        		
        		if (bsp.IsErrorOccured())  {
        			
        			runOnUiThread(new Runnable() {
						
						public void run() {
							msg = "NBioBSP ExportAudit Error: " + bsp.GetErrorCode();
							tvInfo.setText(msg);
							Toast.makeText(Android_Demo.this, msg, Toast.LENGTH_SHORT).show();
						}
					});
        			
            		return ;
            	}
        		
        		if (byCapturedRaw2 != null)
        			byCapturedRaw2 = null;
        		
        		byCapturedRaw2 = new byte[exportAudit.FingerData[0].Template[0].Data.length];
        		byCapturedRaw2 = exportAudit.FingerData[0].Template[0].Data;
    			
    			nCapturedRawWidth2 = exportAudit.ImageWidth;
    			nCapturedRawHeight2 = exportAudit.ImageHeight;
    			
				msg = "First Capture Success";
        	}

        }
    	
    	runOnUiThread(new Runnable() {
			
			public void run() {
				tvInfo.setText(msg);
				
				if (byTemplate1 != null && byTemplate2 != null)  {
					btnVerifyTemplate.setEnabled(true);
				}else{
					btnVerifyTemplate.setEnabled(false);
				}
				
				if (byCapturedRaw1 != null && byCapturedRaw2 != null)  {
					btnVerifyRaw.setEnabled(true);
				}else{
					btnVerifyRaw.setEnabled(false);
				}
				
			}
		});

	}
	
	/**
	 * void
	 */
	public void OnBtnCapture2(View target) {
		
		OnCapture2(10000);
		
	}
	
    /**
     * void
     */
    public void OnBtnVerifyIso(View target){
    	String msg = "";

    	if (byTemplate1 != null && byTemplate2 != null)  {
    		NBioBSPJNI.FIR_HANDLE hLoadFIR1, hLoadFIR2;
    		
    		{
    			hLoadFIR1 = bsp.new FIR_HANDLE();
    			
    			exportEngine.ImportFIR(byTemplate1, byTemplate1.length, NBioBSPJNI.EXPORT_MINCONV_TYPE.OLD_FDA, hLoadFIR1);
    			
    			if (bsp.IsErrorOccured())  {
    				msg = "Template NBioBSP ImportFIR Error: " + bsp.GetErrorCode();
            		tvInfo.setText(msg);
            		return ;
    			}
    		}
    		
    		{
    			hLoadFIR2 = bsp.new FIR_HANDLE();
    			
    			exportEngine.ImportFIR(byTemplate2, byTemplate2.length, NBioBSPJNI.EXPORT_MINCONV_TYPE.OLD_FDA, hLoadFIR2);
    			
    			if (bsp.IsErrorOccured())  {
    				hLoadFIR1.dispose();
    				msg = "Template NBioBSP ImportFIR Error: " + bsp.GetErrorCode();
            		tvInfo.setText(msg);
            		return ;
    			}
    		}
    		
    		// Verify Match
    		NBioBSPJNI.INPUT_FIR inputFIR1, inputFIR2;
    		Boolean bResult = new Boolean(false);
    		
    		inputFIR1 = bsp.new INPUT_FIR();
    		inputFIR2 = bsp.new INPUT_FIR();
    		
    		inputFIR1.SetFIRHandle(hLoadFIR1);
    		inputFIR2.SetFIRHandle(hLoadFIR2);
    		
    		bsp.VerifyMatch(inputFIR1, inputFIR2, bResult, null);
    		 		
    		if (bsp.IsErrorOccured())  {
    			msg = "Template NBioBSP VerifyMatch Error: " + bsp.GetErrorCode();
    		}else  {
    			if (bResult){
    				msg = "Template VerifyMatch Successed";    				
    			}else{
    				msg = "Template VerifyMatch Failed";    				
    			}
    		}
    		
    		hLoadFIR1.dispose();
    		hLoadFIR2.dispose();
    	}else{
    		msg = "Can not find captured data";
    	}
    	
		tvInfo.setText(msg);
    }
    
    /**
     * void
     */
    public void OnBtnVerifyRaw(View target){
    	
    	String msg = "";
    	if (byCapturedRaw1 != null && byCapturedRaw2 != null)  {
    		NBioBSPJNI.FIR_HANDLE hLoadAudit1, hLoadAudit2;
    		NBioBSPJNI.FIR_HANDLE hPorcessedFIR1, hPorcessedFIR2; 
    		
    		{
    			NBioBSPJNI.Export.AUDIT importAudit = exportEngine.new AUDIT();
    			
    			importAudit.FingerNum = (byte) 1;
    			importAudit.SamplesPerFinger = 1;
    			importAudit.ImageWidth = nCapturedRawWidth1;
    			importAudit.ImageHeight = nCapturedRawHeight1;
    			importAudit.FingerData = new NBioBSPJNI.Export.FINGER_DATA[importAudit.FingerNum];
    			importAudit.FingerData[0] = exportEngine.new FINGER_DATA();
    			importAudit.FingerData[0].Template = new NBioBSPJNI.Export.TEMPLATE_DATA[importAudit.SamplesPerFinger];
				importAudit.FingerData[0].Template[0] = exportEngine.new TEMPLATE_DATA();
				importAudit.FingerData[0].FingerID = NBioBSPJNI.FINGER_ID.UNKNOWN;
				importAudit.FingerData[0].Template[0].Data = new byte[byCapturedRaw1.length];
				importAudit.FingerData[0].Template[0].Data = byCapturedRaw1;
    			
				hLoadAudit1 = bsp.new FIR_HANDLE();
    			
    			exportEngine.ImportAudit(importAudit, hLoadAudit1);
    			
    			if (bsp.IsErrorOccured())  {
    				msg = "RawData NBioBSP ImportAudit Error: " + bsp.GetErrorCode();
            		tvInfo.setText(msg);
            		return ;
    			}
    			
    			hPorcessedFIR1 = bsp.new FIR_HANDLE();
    			NBioBSPJNI.INPUT_FIR inputFIR = bsp.new INPUT_FIR();
    			inputFIR.SetFIRHandle(hLoadAudit1);
    			bsp.Process(inputFIR, hPorcessedFIR1);
    			
    			if (bsp.IsErrorOccured())  {
    				hLoadAudit1.dispose();
    				msg = "RawData NBioBSP Process Error: " + bsp.GetErrorCode();
            		tvInfo.setText(msg);
            		return ;
    			}
    		}
    		
    		{
    			NBioBSPJNI.Export.AUDIT importAudit = exportEngine.new AUDIT();
    			
    			importAudit.FingerNum = (byte) 1;
    			importAudit.SamplesPerFinger = 1;
    			importAudit.ImageWidth = nCapturedRawWidth2;
    			importAudit.ImageHeight = nCapturedRawHeight2;
    			importAudit.FingerData = new NBioBSPJNI.Export.FINGER_DATA[importAudit.FingerNum];
    			importAudit.FingerData[0] = exportEngine.new FINGER_DATA();
    			importAudit.FingerData[0].Template = new NBioBSPJNI.Export.TEMPLATE_DATA[importAudit.SamplesPerFinger];
				importAudit.FingerData[0].Template[0] = exportEngine.new TEMPLATE_DATA();
				importAudit.FingerData[0].FingerID = NBioBSPJNI.FINGER_ID.UNKNOWN;
				importAudit.FingerData[0].Template[0].Data = new byte[byCapturedRaw2.length];
				importAudit.FingerData[0].Template[0].Data = byCapturedRaw2;
    			
				hLoadAudit2 = bsp.new FIR_HANDLE();
    			
    			exportEngine.ImportAudit(importAudit, hLoadAudit2);
    			
    			if (bsp.IsErrorOccured())  {
    				hLoadAudit1.dispose();
    				msg = "RawData NBioBSP ImportAudit Error: " + bsp.GetErrorCode();
            		tvInfo.setText(msg);
            		return ;
    			}
    			
    			NBioBSPJNI.INPUT_FIR inputFIR = bsp.new INPUT_FIR();
    			hPorcessedFIR2 = bsp.new FIR_HANDLE();
    			inputFIR.SetFIRHandle(hLoadAudit2);
    			bsp.Process(inputFIR, hPorcessedFIR2);
    			
    			if (bsp.IsErrorOccured())  {
    				hLoadAudit1.dispose();
    				hPorcessedFIR1.dispose();
    				hLoadAudit2.dispose();
    				msg = "RawData NBioBSP Process Error: " + bsp.GetErrorCode();
            		tvInfo.setText(msg);
            		return ;
    			}
    		}
    		
    		NBioBSPJNI.INPUT_FIR inputFIR1, inputFIR2;
    		Boolean bResult = new Boolean(false);
    		
    		inputFIR1 = bsp.new INPUT_FIR();
    		inputFIR2 = bsp.new INPUT_FIR();
    		
    		inputFIR1.SetFIRHandle(hPorcessedFIR1);
    		inputFIR2.SetFIRHandle(hPorcessedFIR2);
    		
    		bsp.VerifyMatch(inputFIR1, inputFIR2, bResult, null);
    		 		
    		if (bsp.IsErrorOccured())  {
    			msg = "RawData NBioBSP VerifyMatch Error: " + bsp.GetErrorCode();
    		}else  {
    			if (bResult){    				
    				msg = "RawData VerifyMatch Successed";
    			}else{    				
    				msg = "RawData VerifyMatch Failed";
    			}
    		}
    		
    		hLoadAudit1.dispose();
    		hLoadAudit2.dispose();
    		hPorcessedFIR1.dispose();
    		hPorcessedFIR2.dispose();
    		
    	}else{
    		msg = "Can not find captured data";
    	}
    	
		tvInfo.setText(msg);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

    	MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_android__demo, menu);
    	
    	return true;
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

    	switch(item.getItemId()){
    	case R.id.menu_add_fir:
    		
    		userDialog.show(getFragmentManager(), "add_fir");		    	
    		
    		
    		return true;
    	case R.id.menu_identify:
    		
    		OnIdentify(5000);
    		
    		return true;
    	case R.id.menu_remove:
    		userDialog.show(getFragmentManager(), "remove");		    	
    		
    		return true;
    	}
    	
    	return false;
    }
    
	public synchronized void OnAddFIR(int timeout, String id){

		NBioBSPJNI.FIR_HANDLE hCapturedFIR, hAuditFIR;
    	NBioBSPJNI.CAPTURED_DATA capturedData;
    	hCapturedFIR = bsp.new FIR_HANDLE();
    	hAuditFIR = bsp.new FIR_HANDLE();
    	capturedData = bsp.new CAPTURED_DATA();
		
    	bsp.Capture(NBioBSPJNI.FIR_PURPOSE.ENROLL,hCapturedFIR,timeout, hAuditFIR, capturedData,  mCallback,0,null);

		if (bsp.IsErrorOccured())  {
        	msg = "NBioBSP Capture Error: " + bsp.GetErrorCode();
        }else  {

        	NBioBSPJNI.INPUT_FIR inputFIR;
        	
        	inputFIR = bsp.new INPUT_FIR();
        	
        	inputFIR.SetFIRHandle(hCapturedFIR);

    		SAMPLE_INFO sampleInfo = indexSearch.new SAMPLE_INFO();
    		
    		indexSearch.AddFIR(inputFIR, Integer.parseInt(id), sampleInfo);
    		if (bsp.IsErrorOccured())  {
    			
    			Toast.makeText(this, id+ " Add Failure", Toast.LENGTH_SHORT).show();
    		}else{
    			
    			Toast.makeText(this, id+ " Add Success", Toast.LENGTH_SHORT).show();
    		}

        }

	}
	
	public synchronized void OnIdentify(int timeout){
		
		NBioBSPJNI.FIR_HANDLE hCapturedFIR, hAuditFIR;
    	NBioBSPJNI.CAPTURED_DATA capturedData;
    	hCapturedFIR = bsp.new FIR_HANDLE();
    	hAuditFIR = bsp.new FIR_HANDLE();
    	capturedData = bsp.new CAPTURED_DATA();
		
    	bsp.Capture(NBioBSPJNI.FIR_PURPOSE.ENROLL,hCapturedFIR,timeout, hAuditFIR, capturedData,  mCallback,0,null);

		if (bsp.IsErrorOccured())  {
        	msg = "NBioBSP Capture Error: " + bsp.GetErrorCode();
        }else  {

        	NBioBSPJNI.INPUT_FIR inputFIR;
        	
        	inputFIR = bsp.new INPUT_FIR();
        	
        	inputFIR.SetFIRHandle(hCapturedFIR);

        	FP_INFO fpInfo = indexSearch.new FP_INFO();
    		
    		indexSearch.Identify(inputFIR, 1, fpInfo, 2000);
    		
    		if(fpInfo.ID!=0){    			
    			Toast.makeText(this, fpInfo.ID+" Identify Success", Toast.LENGTH_SHORT).show();
    		}else{
    			Toast.makeText(this, "Identify Failure", Toast.LENGTH_SHORT).show();
    		}

        }	
		
	}
	
	public synchronized void OnRemoveUser(String id){
		
		indexSearch.RemoveUser(Integer.parseInt(id));
		if (bsp.IsErrorOccured())  {			
			Toast.makeText(this, id+" Delete Failure", Toast.LENGTH_SHORT).show();
		}else{
			
			Toast.makeText(this, id+" Delete Success", Toast.LENGTH_SHORT).show();
		}
		
	}

	/* (non-Javadoc)
	 * @see com.nitgen.SDK.AndroidBSP.UserDialog.UserDialogListener#onClickPositiveBtn(android.app.DialogFragment, java.lang.String)
	 */
	public void onClickPositiveBtn(DialogFragment dialogFragment, String id) {

		if("add_fir".equals(dialogFragment.getTag())){
			OnAddFIR(5000, id);
		}else if("remove".equals(dialogFragment.getTag())){
			OnRemoveUser(id);
		}
		
	}

}