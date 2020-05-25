import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;


public class Receiver {
	
	// probabilidade de perda de ack
	public static final double PROBABILITY = 0;

	public static void main(String[] args) throws Exception{
		
		DatagramSocket fromSender = new DatagramSocket(9888);
		
		//83 é o tamanho base(em bytes) de um rdtpacket serializado
		byte[] receivedData = new byte[Sender.MSS + 83];
		
		int waitingFor = 0;
		
		//lista de pacotes recebidos
		ArrayList<RDTPacket> received = new ArrayList<RDTPacket>();
		
		//variavel de controle auxiliar
		boolean end = false;
		
		while(!end){
			
			System.out.println("Esperando por pacote");
			
			//Recebe pacote
			DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
			fromSender.receive(receivedPacket);
			
			// deserializa para um rdtpacket
			RDTPacket packet = (RDTPacket) Serializer.toObject(receivedPacket.getData());
			
			System.out.println("Pacote com numero de sequencia: " + packet.getSeq() + " Recebido (ultimo: " + packet.isLast() + " )");
		
			if(packet.getSeq() == waitingFor && packet.isLast()){
				
				waitingFor++;
				received.add(packet);
				
				System.out.println("Ultimo pacote recebido");
				
				end = true;
				
			}else if(packet.getSeq() == waitingFor){
				waitingFor++;
				received.add(packet);
				System.out.println("Pacote armazenado no buffer");
			}else{
				System.out.println("Pacote descartado (fora de ordem)");
			}
			
			// cria objeto rdtack
			RDTAck ackObject = new RDTAck(waitingFor);
			
			// serializa objeto
			byte[] ackBytes = Serializer.toBytes(ackObject);
			
			
			DatagramPacket ackPacket = new DatagramPacket(ackBytes, ackBytes.length, receivedPacket.getAddress(), receivedPacket.getPort());
			
			// envia com probabilidade de perda estabelecida
			if(Math.random() >= PROBABILITY){
				fromSender.send(ackPacket);
			}else{
				System.out.println("Perda de ACK com numero de sequencia: " + ackObject.getPacket());
			}
			
			System.out.println("Enviando ACK para seq: " + waitingFor + " com " + ackBytes.length  + " bytes");
			

		}
		
		//imprime dados recebidos
		System.out.println(" ------------ DADOS ---------------- ");
		
		for(RDTPacket p : received){
			for(byte b: p.getData()){
				System.out.print((char) b);
			}
		}
		
	}
	
	
}
