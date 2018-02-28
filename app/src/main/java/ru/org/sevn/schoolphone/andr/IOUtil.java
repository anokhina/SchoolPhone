/*
 * Copyright 2018 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.org.sevn.schoolphone.andr;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class IOUtil {
    public static File getExternalFile(boolean write, String relativePath) {
        File root = getExternalDir(write);
        if (root != null) {
            return new File(root, relativePath);
        }
        return null;
    }
    public static File getExternalDir(boolean write) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory();
        } else if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            if (!write) {
                return Environment.getExternalStorageDirectory();
            }
        }
        return null;
    }
    public static File prepareParent(File fl) {
        File parent = fl.getParentFile();
        if (parent.exists()) {} else {
            parent.mkdirs();
        }
        return fl;
    }
    public static String readExt(String fileName) {
        File fl = getExternalFile(false, fileName);
        return readExt(fl);
    }
    public static String readExt(File fl) {
        if (fl != null && fl.exists() && fl.canRead()) {
            SBTextFileLineProcessor processor = new SBTextFileLineProcessor();
            try {
                importTextFile(fl, processor);
                return processor.getStringBuilder().toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static boolean saveExt(String fileName, byte[] fileData) {
        return saveExt(fileName, fileData, false);
    }
    public static boolean saveExt(String fileName, byte[] fileData, boolean append) {
        File fl = getExternalFile(true, fileName);
        if (fl != null) {
            return save(prepareParent(fl), fileData, append);
        }
        return false;
    }
    public static boolean save(File file2write, byte[] fileData, boolean append) {

        boolean isFileSaved = false;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file2write, append);
            fos.write(fileData);
            fos.flush();
            isFileSaved = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return isFileSaved;
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static String FILE_ENCODING = "UTF-8";

    public static class SBTextFileLineProcessor implements TextFileLineProcessor {
        private StringBuilder sb = new StringBuilder();

        @Override
        public boolean processLine(String s) {
            sb.append(s).append("\n");
            return true;
        }

        public StringBuilder getStringBuilder() {
            return sb;
        }
    }
    public interface TextFileLineProcessor {
        boolean processLine(String s);
    }

    public static long importTextFile(final File file, final TextFileLineProcessor fileLineProcessor) throws IOException {
        long ret = 0;
        FileInputStream fisFileName = null;
        try {
            fisFileName = new FileInputStream(file);
            if (fileLineProcessor != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(fisFileName, FILE_ENCODING));
                for (String lOrig=br.readLine(); lOrig != null;  lOrig=br.readLine()) {
                    ret++;
                    if (!fileLineProcessor.processLine(lOrig)) {
                        break;
                    }
                }
            }
        } finally {
            if (fisFileName != null) {
                try {
                    fisFileName.close();
                } catch (Exception e) {}
            }
        }
        return ret;
    }

}
