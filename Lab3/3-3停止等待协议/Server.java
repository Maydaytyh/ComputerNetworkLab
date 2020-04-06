package network.chapter3.lab3;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import network.*;
import network.chapter3.lab1.CRC_CCITT;

public class Server extends Thread{
	// ������Щ����������ӿڣ����ݣ����ݷ���֡�ı�ţ�
	private int NextFrameToSend = 0;
	private String IpAddress = "";
	private int UDPPort=8888;
	private Frame InfoFrame;
	private int FilterError;
	private int FilterLost;
	private int NumOfInfos = 0;
	private byte[] Buffer=new byte[4];
	private File SendFile;
	private boolean EndOfFile = false;
	private boolean KeepId = false;
	private int len;
	private int AckId=0;
	// ��ȡ��д�����
	// ObjectInputStream in;
	BufferedReader in;
	ObjectOutputStream out;
	DataOutputStream dos;
	public Server(int UDPPort, int FilterError, int FilterLost, String FilePath) {
		this.NextFrameToSend = 0;
		this.IpAddress = "127.0.0.1";
		this.UDPPort = UDPPort;
		this.FilterError = FilterError;
		this.FilterLost = FilterLost;
		this.SendFile = new File(FilePath);
		CRC_CCITT CRC = null;
		Socket Client=null;
		// ���ö�ȡ�ļ�
		FileInputStream ReadStream = null;
		try {
			ReadStream = new FileInputStream(this.SendFile);
			System.out.println("Server:��������ȡ�ļ��ɹ�!\n");
		} catch (FileNotFoundException e) {
			System.out.println("Server: ��ȡ�ļ�����\n");
			System.out.println("Server: FileNotFoundException :" + e.toString());
		}
		 
		// ��ȡ���ݵ�������
		try {
			
			while ((EndOfFile == false)) 
			{
				if (KeepId == false) {
					len=ReadStream.read(Buffer);
					if (len== -1) {
						EndOfFile = true;
						continue;
					}
//					System.out.println("Server:����Ϊ"+len);
					InfoFrame = new Frame(NextFrameToSend, FilterError, FilterLost);
					InfoFrame.SetMsg(Buffer,len);
				}
				else
				{
					KeepId = false;
					InfoFrame = new Frame(NextFrameToSend, FilterError, FilterLost);
					InfoFrame.SetMsg(Buffer, len);
					InfoFrame.SetState("OK");
				}
					
				
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				ObjectOutputStream out;
				// ��Frame����תΪ�������ַ���������crc
				out = new ObjectOutputStream(os);
				out.writeObject(InfoFrame);
				byte[] temp = os.toByteArray();
				out.flush();
				out.close();
				out=null;
				String HexString=CRC_CCITT.toHexString(temp);
				String BinString = CRC_CCITT.byteArrToBinStr(temp);
				String CRCValue = CRC_CCITT.getCRC16CCITT(BinString, 0x1021, 0x0000);
//				System.out.println("Server:ת��ǰ"+CRCValue);
//				System.out.println("Server:����CRCΪ"+CRC_CCITT.getCRC16CCITT(BinString+CRCValue, 0x1021, 0x0000));
				// ���м����t���п���Ϣ��CRC
				String HexCRC=new BigInteger(CRCValue,2).toString(16);
//				System.out.println("Server:CRCת����Ϊ"+HexCRC+"CRCת��ǰΪ"+CRCValue);
				String SendInfo=HexString+"t"+HexCRC+"\n";
				String State = InfoFrame.GetState();
//				//testCRC
//				{
//					System.out.println("                Server:��ʼ����CRC");
//					String Bin1=new BigInteger(HexString,16).toString(2);
//					String CRC1=new BigInteger(HexCRC,16).toString(2);
//					while(CRC1.length()%8!=0) CRC1="0"+CRC1;
//					System.out.println("Server:ת����Ϊ"+CRC1);
//					System.out.println("Server: ���� CRCΪ"+CRC_CCITT.getCRC16CCITT(Bin1+CRC1, 0x1021, 0x0000));
//				}
				Client = new Socket();
				InetSocketAddress ReceiveAdd = new InetSocketAddress(this.IpAddress, this.UDPPort);
				Client.connect(ReceiveAdd, 10000);
				Client.setSoTimeout(15000);
				dos = new DataOutputStream(Client.getOutputStream());
//				System.out.println("       Server:״̬Ϊ"+InfoFrame.GetState());
				if (!(State.equals("Lost"))) {
					// �����ɵĴ�CRC���ַ������
					
					dos.writeBytes(SendInfo);
//					System.out.println("Server:"+SendInfo);
//					dos.close();
					System.out.println("Server:���ڷ���֡���Ϊ" + InfoFrame.GetId());
					System.out.println("Server:next_frame_to_sendΪ" + NextFrameToSend+"\n");
				}
				else {
					try {
						this.sleep(13000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				in = new BufferedReader(new InputStreamReader(Client.getInputStream()));
				String FeedBack = "";
				FeedBack = in.readLine();
//				System.out.println("Server:����Ϊ"+FeedBack);
				// ���ݷ�����ͬ��������
				if (FeedBack.equals("ERROR")) {
					System.out.println("Server:���յ�ACK��ACK���Ϊ:"+AckId);
					AckId++;
					System.out.println("Server:ACK����Ϊ�� �������ݴ������·��͡���\n");
					KeepId = true;
					dos.flush();
					Client.close();
					Client=null;
					InfoFrame=null;
					continue;
				} else if (FeedBack.equals("LOST")) {
					System.out.println("Server:���յ�ACK��ACK���Ϊ:"+AckId);
					AckId++;
					System.out.println("Server: ACK����Ϊ�� �������ݶ�ʧ�����·��͡���\n");
					KeepId = true;
					dos.flush();
					Client.close();
					Client=null;
					InfoFrame=null;
					continue;
				} else {
					System.out.println("Server:���յ�ACK��ACK���Ϊ:"+AckId);
					AckId++;
					System.out.println("Server:ACK ����Ϊ������������ȷ���գ�������һ֡����\n");
					NextFrameToSend++;
				}
				dos.flush();
				Client.close();
				Client=null;
				InfoFrame=null;
			} 
			}
		catch (IOException e1) {
//				System.out.println("Server: Socket���ӳ���!");
				System.out.println("Server: IOException :" + e1.toString());
			}
		//����
		System.out.println("Server:������ϣ�\n");
		InfoFrame = new Frame(NextFrameToSend, FilterError, FilterLost);
		InfoFrame.SetState("END");
		try {
			//ת���ʽ
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ObjectOutputStream out;
			// ��Frame����תΪ�������ַ���������crc
			out = new ObjectOutputStream(os);
			out.writeObject(InfoFrame);
			byte[] temp = os.toByteArray();
			out.flush();
			out.close();
			out=null;
			String HexString=CRC_CCITT.toHexString(temp);
			String BinString = CRC_CCITT.byteArrToBinStr(temp);
			String CRCValue = CRC_CCITT.getCRC16CCITT(BinString, 0x1021, 0x0000);
			// ���м����t���п���Ϣ��CRC
			String HexCRC=new BigInteger(CRCValue,2).toString(16);
			String SendInfo=HexString+"t"+HexCRC+"\n";
//			System.out.println("Server:�������� ");
			
			//��ʼ���ͽ�����Ϣ
			Client = new Socket();
			InetSocketAddress ReceiveAdd = new InetSocketAddress(this.IpAddress, this.UDPPort);
			Client.connect(ReceiveAdd, 10000);
			dos = new DataOutputStream(Client.getOutputStream());
//			System.out.println("       Server:״̬Ϊ"+InfoFrame.GetState());
			dos.writeBytes(SendInfo);
//			System.out.println("Server:"+SendInfo);
			in = new BufferedReader(new InputStreamReader(Client.getInputStream()));
			String FeedBack = "";
			FeedBack = in.readLine();
			System.out.println("Server:��������״̬Ϊ"+FeedBack+"\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		}		
	}
