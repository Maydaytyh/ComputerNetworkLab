package network.chapter3.lab3;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import network.chapter3.lab1.CRC_CCITT;

import javax.jws.soap.SOAPBinding.Use;

public class Client extends Thread {
    private static final String GetInfo = null;
	private static ServerSocket Server;
    private static int ServerPort = 8888;
    private int ExpectedFrameId;
    private byte[] Buffer=new byte[4];
    private String SavePath;
    private byte[] FileInfo=new byte[0];
    private Frame InfoFrame;
    private boolean EndOfFile = false;
    private String FileName = "test.txt";
    Socket UsedSocket;
    // ObjectInputStream in;
    BufferedReader in;

    public Client(int UDPPort, String SavePath) {
        this.SavePath = SavePath;
        ServerPort = UDPPort;
        try {
            Server = new ServerSocket(ServerPort);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            System.out.println("Client: Socket��������!");
            System.out.println("IOException :" + e.toString());
        }
        ExpectedFrameId = 0;
        EndOfFile = false;
    }

    public void run() {

        System.out.println("Client:�ͻ���������");
        while (!EndOfFile) {
        	UsedSocket = null;
            try {
            	 
                 synchronized (Server) {
                	 UsedSocket = Server.accept();
//                     System.out.println("Client:���ӳɹ���");
                 }
                // ���ó�ʱ�������ȡ֡��ʧ��״̬
                UsedSocket.setSoTimeout(15000);
                in = new BufferedReader(new InputStreamReader(UsedSocket.getInputStream()));
//                DataInputStream dos=new DataInputStream(UsedSocket.getInputStream());
                
                String GetInfo = in.readLine();
//                in.close();
//                String GenInfo=dos.readUTF();
//                System.out.println("Client:"+GetInfo);
//                in.close();
//                in = null;
                // ���ַ�����Ϊ��Ϣ�ֽ�����CRC����������
                String TrueInfo = GetInfo.split("t")[0];
                String CRCInfo = GetInfo.split("t")[1];
                // ��Frame������з����л�
//                byte[] ByteInfo = TrueInfo.getBytes();
                byte[] ByteInfo=CRC_CCITT.parseHexStr2Byte(TrueInfo);
//                System.out.println("Client:TrueInfo"+ByteInfo.toString());
                // ������Ŷ��������ݵ�API
                ByteArrayInputStream byteArrayInputStream = null;
                // ���������л�����
                ObjectInputStream objectInputStream = null;
                // ���з����л�����ȡInfoFrame����
                byteArrayInputStream = new ByteArrayInputStream(ByteInfo);
                objectInputStream = new ObjectInputStream(byteArrayInputStream);
                InfoFrame=(Frame) objectInputStream.readObject();
                System.out.println("Client:���յ�֡���Ϊ" + InfoFrame.GetId());
                // �жϻ�ȡ��֡��״̬
                String FrameState = InfoFrame.GetState();
                // ����״̬
                if (FrameState.equals("END")) {
                    EndOfFile = true;
                    DataOutputStream dos1 = new DataOutputStream(UsedSocket.getOutputStream());
                    dos1.writeBytes("END" + '\n');
                    dos1.close();
                    System.out.println("Client:��������");
                    
                    continue;
                } else {
                	//תΪ2�����ַ���
                	TrueInfo=new BigInteger(TrueInfo,16).toString(2);
                	CRCInfo=new BigInteger(CRCInfo,16).toString(2);
                	while(CRCInfo.length()%8!=0)
                		CRCInfo="0"+CRCInfo;
//                	CRCInfo=CRC_CCITT.byteArrToBinStr(CRC_CCITT.parseHexStr2Byte(CRCInfo));
                    String CRCValue = CRC_CCITT.getCRC16CCITT(TrueInfo+CRCInfo, 0x1021, 0x0000);
                    // �����޴�
                    System.out.println("CLient:CRCValue="+CRCValue);
                    if (Integer.valueOf(CRCValue,2) == 0) {
//                    	synchronized (Server) {
//                       	 UsedSocket = Server.accept();
//                        }
                    	Buffer=InfoFrame.GetMsg();
                    	// ������д�뽫������ļ�����
                        byte[] Data = new byte[Buffer.length + FileInfo.length];
//                        System.arraycopy(InfoFrame.GetMsg(), 0, Data, 0, 4);
//                        System.arraycopy(FileInfo, 0, Data, 4, FileInfo.length);
                        System.arraycopy(FileInfo,0,Data,0,FileInfo.length);
                        System.arraycopy(Buffer, 0, Data, FileInfo.length, InfoFrame.Length);
                        FileInfo = Data;
                        System.out.println("Client:�����޴����յ���֡���Ϊ��" + InfoFrame.GetId());
                        // ACK״̬ΪOK
                        DataOutputStream dos1 = new DataOutputStream(UsedSocket.getOutputStream());
                        dos1.writeBytes("OK" + '\n');
                        dos1.close();
                        
                    }
                    // ���ݴ���
                    else {
                        DataOutputStream dos1 = new DataOutputStream(UsedSocket.getOutputStream());
                        dos1.writeBytes("ERROR" + '\n');
                        dos1.close();
                        System.out.println("Clinet:���ݳ���");
                    }
                }
                in.close();
                in = null;
                UsedSocket.close();
                UsedSocket=null;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (java.net.SocketTimeoutException e) {
                System.out.println("Client��֡��ʧ");
                try {
                    DataOutputStream dos = new DataOutputStream(UsedSocket.getOutputStream());
                    dos.writeBytes("LOST" + '\n');
                    dos.close();
                } catch (java.io.IOException ee) {
                    ee.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        System.out.println("Client:�ļ�������ϣ�");
        // ��byte����洢���ļ���
        File RecFile = new File(SavePath + FileName);
        try {
            FileOutputStream FileOut = new FileOutputStream(RecFile);
            FileOut.write(FileInfo);
            FileOut.flush();
            FileOut.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            System.out.println("Client:�޷��������ļ���");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Client:д���ļ�ʧ�ܣ�");
        }
    }
}