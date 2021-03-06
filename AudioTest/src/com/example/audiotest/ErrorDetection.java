package com.example.audiotest;

public class ErrorDetection {

    public static final int         CHECKSUM_ERROR  = -1000;
    private static final int        PARITY_EVEN     = 64;
    private static final int        PARITY_ODD              = 32;

    private static int checkSum(int number){
        int counter = Integer.bitCount(number);
        int sign = (int)Math.pow(-1, counter);
        if (sign>0)
            return PARITY_EVEN; //even
        else
            return PARITY_ODD; //odd
    }

    public static int createMessage(int number){
        int cSum = checkSum(number);
        int message = number+cSum;
        return message;
    }
    public static int decodeMessage(int message){
        int mask = 31;// 00011111
        int number = message&mask;
        int check = message-number;
        int cSum = checkSum(number);
        if ( cSum!=check)
            return CHECKSUM_ERROR;
        else
            return number;
    }

    public static void main(String[] args){
        int n = 2;
        System.out.println("number=" + n + " binary=" + Integer.toBinaryString(n));
        n = createMessage(n);
        System.out.println("msg=" + n + " binary=" + Integer.toBinaryString(n));
        n = decodeMessage(n);
        System.out.println("decoded=" + n + " binary=" + Integer.toBinaryString(n));
        n = 68;
        n = decodeMessage(n);
        System.out.println("decoded=" + n + " binary=" + Integer.toBinaryString(n));
        // testing encoding-decoding
        for (int i = 0; i < 32; i++) {
            n = i;
            System.out.print("number=" + n + " binary=" + Integer.toBinaryString(n));
            n = createMessage(i);
            System.out.println("msg=" + n + " binary=" + Integer.toBinaryString(n));
            n = decodeMessage(n);
            System.out.println("decoded=" + n + " binary=" + Integer.toBinaryString(n));
        }
    }

}