package com.oliver.a5bluetooth;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;

public class FirmwareManager
{
    private static final String BINARY = "B";
    private static final String CHECKSUM = "C";
    private byte[] updateFileBinaryData = null;
    private long fileSize = 0;
    private long checkSum = 0;
    private final int BYTE_STEP = 20;
    private int start,end;
    private boolean first = false;
    ArrayList<byte[]> tasksToExecute;
    int taskIndex = 0;

    /**
     * @param filePath
     *            - path to the file
     */
    public FirmwareManager(String filePath) {
        this.tasksToExecute = new ArrayList<byte[]>();
        this.start = 0;
        this.end = BYTE_STEP;
        try {
            updateFileBinaryData = this.readFile(new File(filePath));
            calculateCheckSum();
            initTaskList();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void initTaskList() {
        tasksToExecute.add(this.getFileSize().getBytes());
        tasksToExecute.add(this.getCheckSum().getBytes());
        while(end < this.fileSize)
        {
            tasksToExecute.add(this.getByteBlock());
        }
    }

    public byte[] executeTask() {
        return this.tasksToExecute.get(taskIndex++);
    }

    private byte[] getByteBlock() {
        if(first) {
            this.first = true;
            return Arrays.copyOfRange(updateFileBinaryData, start, end);
        }
        else {
            this.start += BYTE_STEP;
            this.end += BYTE_STEP;
            return Arrays.copyOfRange(updateFileBinaryData, start, end);
        }
    }



    private  byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            this.fileSize = f.length();
            int length = (int) longlength;
            if (length != longlength)
                throw new IOException("File size >= 2 GB");
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

    public String getFileSize() {
        return String.valueOf(this.fileSize).trim() + BINARY;
    }

    public String getCheckSum() {
        return String.valueOf(this.checkSum).trim() + CHECKSUM;
    }

    private void calculateCheckSum() {
        for(int index = 0;index < updateFileBinaryData.length;index++) {
            checkSum += unsignedToBytes(updateFileBinaryData[index]);
        }
    }

    private  int unsignedToBytes(byte b) {
        return b & 0xFF;
    }
}

