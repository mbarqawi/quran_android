package com.quran.labs.androidquran.util;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.quran.labs.androidquran.R;
import com.quran.labs.androidquran.common.QuranAyah;
import com.quran.labs.androidquran.data.QuranInfo;
import com.quran.labs.androidquran.service.util.AudioRequest;
import com.quran.labs.androidquran.service.util.DownloadAudioRequest;
import com.quran.labs.androidquran.ui.helpers.QuranRow;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class AudioUtils {
   private static final String TAG = "AudioUtils";
   public static final String DB_EXTENSION = ".db";
   public static final String AUDIO_EXTENSION = ".mp3";
   public static final String ZIP_EXTENSION = ".zip";
   private static String AUDIO_DIRECTORY = "audio";

    public static int getAudioDownloadStatus(Context con, int qariID, String qariUrl, int sura, ArrayList<Integer> AudioInfoArray) {

        //the function will have  three possible returen values
        //1 mean i have the Sura completely
        //2 mean i have partially
        //3 mean I don't have it
        int returnValue = 3;
        if (AudioUtils.isQariGapless(con, qariID)) {
            //do you have the file for this Qari and this sura
            returnValue = AudioUtils.haveSuraForGaplessQari(qariUrl, sura) ? 1 : 3;
        } else {
            int DownloadedAyahCounter = QuranInfo.SURA_NUM_AYAHS[sura - 1];
            if (AudioUtils.haveSuraFolderForQari(qariUrl, sura)) {
                for (int AyahI = 1; AyahI < QuranInfo.SURA_NUM_AYAHS[sura - 1]; AyahI++) {
                    //TODO:remove QuranRowelement
                    boolean isAudioExistForAyah = AudioUtils.haveSuraAndAyahForQari(qariUrl, sura, AyahI);
                    if (isAudioExistForAyah) {
                        if (AudioInfoArray != null)
                            AudioInfoArray.add(AyahI);
                    } else
                        DownloadedAyahCounter--;
                }
            } else {
                DownloadedAyahCounter = 0;
            }
            if (DownloadedAyahCounter < QuranInfo.SURA_NUM_AYAHS[sura - 1]&&DownloadedAyahCounter>0)
                returnValue = 2;
            else if (DownloadedAyahCounter == QuranInfo.SURA_NUM_AYAHS[sura - 1])
                returnValue = 1;
            else
                returnValue = 3;
        }
        return returnValue;
    }


    public final static class LookAheadAmount {
      public static final int PAGE = 1;
      public static final int SURA = 2;
      public static final int JUZ = 3;

      // make sure to update these when a lookup type is added
      public static final int MIN = 1;
      public static final int MAX = 3;
   }

   private static String[] mQariBaseUrls = null;
   private static String[] mQariFilePaths = null;
   private static String[] mQariDatabaseFiles = null;

   public static String getQariUrl(Context context, int position,
                                   boolean addPlaceHolders){
      if (mQariBaseUrls == null){
         mQariBaseUrls = context.getResources()
                 .getStringArray(R.array.quran_readers_urls);
      }

      if (position >= mQariBaseUrls.length || 0 > position){ return null; }
      String url = mQariBaseUrls[position];
      if (addPlaceHolders){
         if (isQariGapless(context, position)){
            Log.d(TAG, "qari is gapless...");
            url += "%03d" + AudioUtils.AUDIO_EXTENSION;
         }
         else { url += "%03d%03d" + AudioUtils.AUDIO_EXTENSION; }
      }
      return url;
   }

   public static String getLocalQariUrl(Context context, int position){
      if (mQariFilePaths == null){
         mQariFilePaths = context.getResources()
                 .getStringArray(R.array.quran_readers_path);
      }

      String rootDirectory = getAudioRootDirectory(context);
      return rootDirectory == null? null :
              rootDirectory + mQariFilePaths[position];
   }

   public static boolean isQariGapless(Context context, int position){
      return getQariDatabasePathIfGapless(context, position) != null;
   }

   public static String getQariDatabasePathIfGapless(Context context,
                                                     int position){
      if (mQariDatabaseFiles == null){
         mQariDatabaseFiles = context.getResources()
                 .getStringArray(R.array.quran_readers_db_name);
      }

      if (position > mQariDatabaseFiles.length){ return null; }

      String dbname = mQariDatabaseFiles[position];
      Log.d(TAG, "got dbname of: " + dbname + " for qari");
      if (TextUtils.isEmpty(dbname)){ return null; }

      String path = getLocalQariUrl(context, position);
      if (path == null){ return null; }
      String overall = path + File.separator +
              dbname + DB_EXTENSION;
      Log.d(TAG, "overall path: " + overall);
      return overall;
   }

   public static boolean shouldDownloadGaplessDatabase(
           Context context, DownloadAudioRequest request){
      if (!request.isGapless()){ return false; }
      String dbPath = request.getGaplessDatabaseFilePath();
      if (TextUtils.isEmpty(dbPath)){ return false; }

      File f = new File(dbPath);
      return !f.exists();
   }

   public static String getGaplessDatabaseUrl(
           Context context, DownloadAudioRequest request){
      if (!request.isGapless()){ return null; }
      int qariId = request.getQariId();

      if (mQariDatabaseFiles == null){
         mQariDatabaseFiles = context.getResources()
                 .getStringArray(R.array.quran_readers_db_name);
      }

      if (qariId > mQariDatabaseFiles.length){ return null; }

      String dbname = mQariDatabaseFiles[qariId] + ZIP_EXTENSION;
      return QuranFileUtils.getGaplessDatabaseRootUrl() + "/" + dbname;
   }

   public static QuranAyah getLastAyahToPlay(QuranAyah startAyah,
                                             int page, int mode,
                                             boolean isDualPages){
      if (isDualPages && mode == LookAheadAmount.PAGE && (page % 2 == 1)){
         // if we download page by page and we are currently in tablet mode
         // and playing from the right page, get the left page as well.
         page++;
      }

      int pageLastSura = 114;
      int pageLastAyah = 6;
      if (page > 604 || page < 0){ return null; }
      if (page < 604){
         int nextPageSura = QuranInfo.PAGE_SURA_START[page];
         int nextPageAyah = QuranInfo.PAGE_AYAH_START[page];

         pageLastSura = nextPageSura;
         pageLastAyah = nextPageAyah - 1;
         if (pageLastAyah < 1){
            pageLastSura--;
            if (pageLastSura < 1){ pageLastSura = 1; }
            pageLastAyah = QuranInfo.getNumAyahs(pageLastSura);
         }
      }

      if (mode == LookAheadAmount.SURA){
         int sura = startAyah.getSura();
         int lastAyah = QuranInfo.getNumAyahs(sura);
         if (lastAyah == -1){ return null; }

         // if we start playback between two suras, download both suras
         if (pageLastSura > sura){
            sura = pageLastSura;
            lastAyah = QuranInfo.getNumAyahs(sura);
         }
         return new QuranAyah(sura, lastAyah);
      }
      else if (mode == LookAheadAmount.JUZ){
         int juz = QuranInfo.getJuzFromPage(page);
         if (juz == 30){
            return new QuranAyah(114, 6);
         }
         else if (juz >= 1 && juz < 30){
            int[] endJuz = QuranInfo.QUARTERS[juz * 8];
            if (pageLastSura > endJuz[0]){
               // ex between jathiya and a7qaf
               endJuz = QuranInfo.QUARTERS[(juz+1) * 8];
            }
            else if (pageLastSura == endJuz[0] &&
                     pageLastAyah > endJuz[1]){
               // ex surat al anfal
               endJuz = QuranInfo.QUARTERS[(juz+1) * 8];
            }

            return new QuranAyah(endJuz[0], endJuz[1]);
         }
      }

      // page mode (fallback also from errors above)
      return new QuranAyah(pageLastSura, pageLastAyah);
   }

   public static boolean shouldDownloadBasmallah(Context context,
                                                 DownloadAudioRequest request){
      if (request.isGapless()){ return false; }
      String baseDirectory = request.getLocalPath();
      if (!TextUtils.isEmpty(baseDirectory)){
         File f = new File(baseDirectory);
         if (f.exists()){
            String filename = 1 + File.separator + 1 + AUDIO_EXTENSION;
            f = new File(baseDirectory + File.separator + filename);
            if (f.exists()){
               android.util.Log.d(TAG, "already have basmalla...");
               return false; }
         }
         else {
            f.mkdirs();
         }
      }

      return doesRequireBasmallah(request);
   }
    public static boolean haveSuraFolderForQari(String baseDir, int sura){
        String folderName = baseDir + File.separator + sura ;

        File f = new File(folderName);
        return f.exists();
    }
   public static boolean haveSuraAndAyahForQari(String baseDir, int sura, int ayah){
        String filename = baseDir + File.separator + sura +
                File.separator + ayah + AUDIO_EXTENSION;
        File f = new File(filename);
        return f.exists();
    }
    public static boolean haveSuraForGaplessQari(String baseDir, int sura){
        String filename = baseDir + File.separator + sura + AUDIO_EXTENSION;
        File f = new File(filename);
        return f.exists();
    }
   private static boolean doesRequireBasmallah(AudioRequest request){
      QuranAyah minAyah = request.getMinAyah();
      int startSura = minAyah.getSura();
      int startAyah = minAyah.getAyah();

      QuranAyah maxAyah = request.getMaxAyah();
      int endSura = maxAyah.getSura();
      int endAyah = maxAyah.getAyah();

      android.util.Log.d(TAG, "seeing if need basmalla...");

      for (int i = startSura; i <= endSura; i++){
         int lastAyah = QuranInfo.getNumAyahs(i);
         if (i == endSura){ lastAyah = endAyah; }
         int firstAyah = 1;
         if (i == startSura){ firstAyah = startAyah; }

         for (int j = firstAyah; j < lastAyah; j++){
            if (j == 1 && i != 1 && i != 9){
               android.util.Log.d(TAG, "need basmalla for " + i + ":" + j);

               return true;
            }
         }
      }

      return false;
   }

   public static boolean haveAllFiles(DownloadAudioRequest request){
      String baseDirectory = request.getLocalPath();
      if (TextUtils.isEmpty(baseDirectory)){ return false; }

      boolean isGapless = request.isGapless();
      File f = new File(baseDirectory);
      if (!f.exists()){
         f.mkdirs();
         return false;
      }

      QuranAyah minAyah = request.getMinAyah();
      int startSura = minAyah.getSura();
      int startAyah = minAyah.getAyah();

      QuranAyah maxAyah = request.getMaxAyah();
      int endSura = maxAyah.getSura();
      int endAyah = maxAyah.getAyah();

      for (int i = startSura; i <= endSura; i++){
         int lastAyah = QuranInfo.getNumAyahs(i);
         if (i == endSura){ lastAyah = endAyah; }
         int firstAyah = 1;
         if (i == startSura){ firstAyah = startAyah; }

         if (isGapless){
            if (i == endSura && endAyah == 0){ continue; }
            String p = request.getBaseUrl();
            String fileName = String.format(Locale.US, p, i);
            Log.d(TAG, "gapless, checking if we have " + fileName);
            f = new File(fileName);
            if (!f.exists()){ return false; }
            continue;
         }

         Log.d(TAG, "not gapless, checking each ayah...");
         for (int j = firstAyah; j <= lastAyah; j++){
            String filename = i + File.separator + j + AUDIO_EXTENSION;
            f = new File(baseDirectory + File.separator + filename);
            if (!f.exists()){ return false; }
         }
      }

      return true;
   }

   public static String getAudioRootDirectory(Context context){
      String s = QuranFileUtils.getQuranBaseDirectory(context);
      return (s == null)? null : s + AUDIO_DIRECTORY + File.separator;
   }

   public static String getOldAudioRootDirectory(Context context){
      File f = null;
      String path = "";
      String sep = File.separator;

      if (android.os.Build.VERSION.SDK_INT >= 8){
         f = context.getExternalFilesDir(null);
         path = sep + "audio" + sep;
      }
      else {
         f = Environment.getExternalStorageDirectory();
         path = sep + "Android" + sep + "data" + sep +
                 context.getPackageName() + sep + "files" + sep + "audio" + sep;
      }

      if (f == null){ return null; }
      return f.getAbsolutePath() + path;
   }



	public static String getAudioSammary(ArrayList<Integer> list)
	{
		ArrayList<AudioRange> list2=new ArrayList<AudioRange>();
		Integer h;

		AudioRange y =new AudioRange();
		while (  list.size()!= 0) {
            h =  list.remove(0);

		    if (y.Start == null)
		    {
		        y.Start = h;
		        continue;
		    }
		    if (y.End == null)
		    {
		        y.End = h;
		        continue;
		    }
		    if ((h - y.End) == 1)
		    {
		        y.End++;
		    }
		    else {
		        list2.add(y);
			    y = new AudioRange();
		        y.Start = h;
		        y.End = h;

		    }
		}
		list2.add(y);

        StringBuilder builder = new StringBuilder();

for ( int i=0;i<list2.size() ;i++)
{
    builder.append("from "+ list2.get(i).Start +" ~ "+ list2.get(i).End );
    builder.append("\r\n");
}

		return builder.toString();
	}

}
