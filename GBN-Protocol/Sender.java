import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.net.*;


public class Sender {

	// Maior tamanho de segmento(maximum segment size) - Quantidade de dados da camada de aplicacao no segmento
	public static final int MSS = 4;

	// probabilidade de perda de pacotes durante envio, sera 1 caso opçao seja seja perda

	// Tamanho da "janela" - repesenta o "n" do algoritmo go-back-n
	// numero de pacotes enviados sem ack
	public static final int WINDOW_SIZE = 2;
	
	// tempo (ms) antes de mandar novamente todos pacotes sem ack 
	public static final int TIMER = 30;


	public static void main(String[] args) throws Exception{

		//Numero de sequencia do ultimo pacote enviado(rcvbase)
		int lastSent = 0;
		
		// numero de sequencia do ultimo pacote com ack
		int waitingForAck = 0;
		
		
		System.out.println("Seja bem vindo ao chat, selecione a opção desejada para transmissão de dados:");
		System.out.println("Digite 1 para realizar tranmissão normal");
		System.out.println("Digite 2 para realizar transmissão lenta");
		System.out.println("Digite 3 para realizar transmissão com perda");
		System.out.println("Digite 4 para realizar transmissão fora de ordem");
		System.out.println("Digite 5 para realizar transmissão duplicada");
		Scanner teclado = new Scanner(System.in);
		int controller;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String msgin = null;
		controller = teclado.nextInt();
		if(controller==1) {
			
			int probabilidadePerda=0;
			System.out.println("Tranmissão normal selecionada");
			System.out.print("Envie sua mensagem: ");
			msgin= br.readLine();
			//lógica de transmisssao normal
			byte[] fileBytes = msgin.getBytes();
			System.out.println("Tamanho da mensagem: " + fileBytes.length + " bytes");

			// ultimo pacote -numero de sequencia
			int lastSeq = (int) Math.ceil( (double) fileBytes.length / MSS);
			System.out.println("Numero de pacotes para enviar: " + lastSeq);

			DatagramSocket toReceiver = new DatagramSocket();

			// Endereço do receiver
			InetAddress receiverAddress = InetAddress.getByName("localhost");
			
			//Lista de todos pacotes enviados
			ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();
			
			//loop de transmissao
			
			//while(true) {
				while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){
					//Vetor para guardar parte dos bytes a serem enviados(mss - tamanho do segmento)
					byte[] filePacketBytes = new byte[MSS];
					
					// Copia segmento de bytes de dados para vetor
					filePacketBytes = Arrays.copyOfRange(fileBytes, lastSent*MSS, lastSent*MSS + MSS);
					
					// Cria objeto rdtpacket (pacote de reliable data transfer - rdt)
					RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);
					
					// serializa pacote rdtpacket(objeto)
					byte[] sendData = Serializer.toBytes(rdtPacketObject);
					
					// cria pacote com especificacoes supracitadas(definidas), endereco de ip definido e porta 9876
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9888 );
					
					System.out.println("Enviado pacote com numero de sequencia " + lastSent +  ", tamanho: " + sendData.length + " bytes");
					
					//adiciona pacote a lista de envio
					sent.add(rdtPacketObject);
					
					// envia pacote com probabilidade previamente configurada de perda(neste caso sera 0)
					if(Math.random() >= probabilidadePerda){
						toReceiver.send(packet);
					}else{
						System.out.println("Perda de pacote com numero de sequencia " + lastSent);
					}
					
					//incrementa ultimo envio
					lastSent++;
					
					// vetor de bytes para ack enviado pelo receiver
					byte[] ackBytes = new byte[40];
					
					// cria pacote para ack
					DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
					
					try{
						//se algum ack nao foi recebido no tempo especifico(vai para o catch)
						toReceiver.setSoTimeout(TIMER);
						
						//recebe pacote
						toReceiver.receive(ack);
						
						// "deserializa" o rdtack(objeto) - transforma em objeto
						RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());
						
						System.out.println("ACK Recebido: " + ackObject.getPacket());
						
						// se este ack e do ultimo pacote, para o sender)
						if(ackObject.getPacket() == lastSeq){
							break;
						}
						
						waitingForAck = Math.max(waitingForAck, ackObject.getPacket());
						
					}catch(SocketTimeoutException e){
						//envia todos enviados que nao receberam packets ack
						
						for(int i = waitingForAck; i < lastSent; i++){
							
							//serializa o objeto rdtpacket
							byte[] sendData2 = Serializer.toBytes(sent.get(i));

							//cria o pacote
							DatagramPacket packet2 = new DatagramPacket(sendData2, sendData2.length, receiverAddress, 9888 );
							
							//envia com probabilidade estabelecida previamente
							if(Math.random() > probabilidadePerda){
								toReceiver.send(packet);
							}else{
								System.out.println("Pacote perdido, numero de sequencia: " + sent.get(i).getSeq());
							}

							System.out.println("Re-enviando pacote com numero de sequencia: " + sent.get(i).getSeq() +  " e tamanho: " + sendData.length + " bytes");
						}
					}
				
					
				}

			//}
		}
			
			
			
			
			if(controller==2) {
			System.out.println("Transmissão lenta selecionada");
			System.out.print("Envie sua mensagem: ");
			msgin= br.readLine();
			//set lógica de transmisssao lenta
			int probabilidadePerda=0;

			byte[] fileBytes = msgin.getBytes();
			System.out.println("Tamanho da mensagem: " + fileBytes.length + " bytes");

			// ultimo pacote -numero de sequencia
			int lastSeq = (int) Math.ceil( (double) fileBytes.length / MSS);
			System.out.println("Numero de pacotes para enviar: " + lastSeq);

			DatagramSocket toReceiver = new DatagramSocket();

			// Endereço do receiver
			InetAddress receiverAddress = InetAddress.getByName("localhost");
			
			//Lista de todos pacotes enviados
			ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();
			
			//loop de transmissao	
				while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){
					//Vetor para guardar parte dos bytes a serem enviados(mss - tamanho do segmento)
					byte[] filePacketBytes = new byte[MSS];
					
					// Copia segmento de bytes de dados para vetor
					filePacketBytes = Arrays.copyOfRange(fileBytes, lastSent*MSS, lastSent*MSS + MSS);
					
					// Cria objeto rdtpacket (pacote de reliable data transfer - rdt)
					RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);
					
					// serializa pacote rdtpacket(objeto)
					byte[] sendData = Serializer.toBytes(rdtPacketObject);
					
					// cria pacote com especificacoes supracitadas(definidas), endereco de ip definido e porta 9876
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9888 );
					
					System.out.println("Enviado pacote com numero de sequencia " + lastSent +  ", tamanho: " + sendData.length + " bytes");
					
					//adiciona pacote a lista de envio
					sent.add(rdtPacketObject);
					
					// envia pacote com probabilidade previamente configurada de perda(neste caso sera 1)
					if(Math.random() >= probabilidadePerda){
						
						toReceiver.send(packet);
					}else{
						System.out.println("Perda de pacote com numero de sequencia " + lastSent);
					}
					
					//incrementa ultimo envio
					lastSent++;
					
					// vetor de bytes para ack enviado pelo receiver
					byte[] ackBytes = new byte[40];
					
					// cria pacote para ack
					DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
					
					try{
						//se algum ack nao foi recebido no tempo especifico(vai para o catch)
						// no caso do envio lento, mudaremos o timer para cair na excecao e gerar o envio lento
						//o valor 5 foi escolhido, porem poderia ser qualquer valor inferior a variavel TIMER
						toReceiver.setSoTimeout((5));
						//recebe pacote
						toReceiver.receive(ack);
						
						// "deserializa" o rdtack(objeto) - transforma em objeto
						RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());
						
						System.out.println("ACK Recebido: " + ackObject.getPacket());
						
						// se este ack e do ultimo pacote, para o sender)
						if(ackObject.getPacket() == lastSeq){
							break;
						}
						
						waitingForAck = Math.max(waitingForAck, ackObject.getPacket());
						
					}catch(SocketTimeoutException e){
						//envia todos enviados que nao receberam packets ack
						System.out.println("Envio lento identificado...");
						for(int i = waitingForAck; i < lastSent; i++){
							
							//serializa o objeto rdtpacket
							byte[] sendData2 = Serializer.toBytes(sent.get(i));

							//cria o pacote
							DatagramPacket packet2 = new DatagramPacket(sendData2, sendData2.length, receiverAddress, 9888 );
							
							//envia com probabilidade estabelecida previamente
							if(Math.random() > probabilidadePerda){
								toReceiver.send(packet);
							}else{
								System.out.println("Pacote perdido, numero de sequencia: " + sent.get(i).getSeq());
							}

							System.out.println("Re-enviando pacote com numero de sequencia: " + sent.get(i).getSeq() +  " e tamanho: " + sendData.length + " bytes");
						}
					}
				
					
				}
		
			}
			
		if(controller==3) {
			System.out.println("Transmissão com perda selecionada");
			System.out.print("Envie sua mensagem: ");
			msgin= br.readLine();
			//set lógica de transmisssao com perda
			//neste caso a variavel probabilidadePerda devera ser maior do que o parametro que a compara no laco if
			int probabilidadePerda=1;
			int executaPerda=0;
			byte[] fileBytes = msgin.getBytes();
			System.out.println("Tamanho da mensagem: " + fileBytes.length + " bytes");

			// ultimo pacote -numero de sequencia
			int lastSeq = (int) Math.ceil( (double) fileBytes.length / MSS);
			System.out.println("Numero de pacotes para enviar: " + lastSeq);

			DatagramSocket toReceiver = new DatagramSocket();

			// Endereço do receiver
			InetAddress receiverAddress = InetAddress.getByName("localhost");
			
			//Lista de todos pacotes enviados
			ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();
			
			//loop de transmissao
			
			//while(true) {
				while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){
					//Vetor para guardar parte dos bytes a serem enviados(mss - tamanho do segmento)
					byte[] filePacketBytes = new byte[MSS];
					
					// Copia segmento de bytes de dados para vetor
					filePacketBytes = Arrays.copyOfRange(fileBytes, lastSent*MSS, lastSent*MSS + MSS);
					
					// Cria objeto rdtpacket (pacote de reliable data transfer - rdt)
					RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);
					
					// serializa pacote rdtpacket(objeto)
					byte[] sendData = Serializer.toBytes(rdtPacketObject);
					
					// cria pacote com especificacoes supracitadas(definidas), endereco de ip definido e porta 9876
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9888 );
					
					System.out.println("Enviado pacote com numero de sequencia " + lastSent +  ", tamanho: " + sendData.length + " bytes");
					
					//adiciona pacote a lista de envio
					sent.add(rdtPacketObject);
					
					// envia pacote com probabilidade previamente configurada de perda(neste caso sera 0)
					if(Math.random() >= probabilidadePerda){
						toReceiver.send(packet);
					}else{
						System.out.println("Perda de pacote com numero de sequencia " + lastSent);
					}
					
					//incrementa ultimo envio
					lastSent++;
					
					// vetor de bytes para ack enviado pelo receiver
					byte[] ackBytes = new byte[40];
					
					// cria pacote para ack
					DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
					
					try{
						//se algum ack nao foi recebido no tempo especifico(vai para o catch)
						toReceiver.setSoTimeout(TIMER);
						
						//recebe pacote
						toReceiver.receive(ack);
						
						// "deserializa" o rdtack(objeto) - transforma em objeto
						RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());
						
						System.out.println("ACK Recebido: " + ackObject.getPacket());
						
						// se este ack e do ultimo pacote, para o sender)
						if(ackObject.getPacket() == lastSeq){
							break;
						}
						
						waitingForAck = Math.max(waitingForAck, ackObject.getPacket());
						
					}catch(SocketTimeoutException e){
						//envia todos enviados que nao receberam packets ack
						
						for(int i = waitingForAck; i < lastSent; i++){
							
							//serializa o objeto rdtpacket
							byte[] sendData2 = Serializer.toBytes(sent.get(i));

							//cria o pacote
							DatagramPacket packet2 = new DatagramPacket(sendData2, sendData2.length, receiverAddress, 9888 );
							
							//envia com probabilidade estabelecida previamente
							//neste caso a probabilidade de perda sera maior do que a variavel executaPerda  
							if(probabilidadePerda >executaPerda){
								toReceiver.send(packet);
							}else{
								System.out.println("Pacote perdido, numero de sequencia: " + sent.get(i).getSeq());
							}

							System.out.println("Re-enviando pacote com numero de sequencia: " + sent.get(i).getSeq() +  " e tamanho: " + sendData.length + " bytes");
						}
					}
				
					
				}
			

			
			
		}
		if(controller==4) {
			System.out.println("Transmissão fora de ordem selecionada");
			System.out.print("Envie sua mensagem: ");
			msgin= br.readLine();
			//set lógica de transmisssao fora de ordem
			//para que os pacotes sejam enviados fora de ordem é necessario que a mensagem tenha mais bytes do que o padrao
			//estabelecido pelo protocolo
			int foraDeOrdem=0;
			int executalaco=1;
			byte[] fileBytes = msgin.getBytes();
			System.out.println("Tamanho da mensagem: " + fileBytes.length + " bytes");

			// ultimo pacote -numero de sequencia
			int lastSeq = (int) Math.ceil( (double) fileBytes.length / MSS);
			System.out.println("Numero de pacotes para enviar: " + lastSeq);

			DatagramSocket toReceiver = new DatagramSocket();

			// Endereço do receiver
			InetAddress receiverAddress = InetAddress.getByName("localhost");
			
			//Lista de todos pacotes enviados
			ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();
			
			//loop de transmissao
			
			//while(true) {
				while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){
					//Vetor para guardar parte dos bytes a serem enviados(mss - tamanho do segmento)
					byte[] filePacketBytes = new byte[MSS];
					
					// Copia segmento de bytes de dados para vetor
					filePacketBytes = Arrays.copyOfRange(fileBytes, lastSent*MSS, lastSent*MSS + MSS);
					
					// Cria objeto rdtpacket (pacote de reliable data transfer - rdt)
					RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);
					
					// serializa pacote rdtpacket(objeto)
					byte[] sendData = Serializer.toBytes(rdtPacketObject);
					
					// cria pacote com especificacoes supracitadas(definidas), endereco de ip definido e porta 9876
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9888 );
					
					System.out.println("Enviado pacote com numero de sequencia " + lastSent +  ", tamanho: " + sendData.length + " bytes");
					
					//adiciona pacote a lista de envio
					sent.add(rdtPacketObject);
					
					//logica que faz com que nao aceita a primeira iteracao
					//porem aceite a segunda
					if(foraDeOrdem >= executalaco){
						toReceiver.send(packet);
					}else{
						System.out.println("Identificando transmissao fora de ordem ");
						System.out.println("Realizando correcao na transmissao...");
						//dessa forma nao sera executado da primeira vez, porem da segunda vez sim
						foraDeOrdem+=2;
						
					}
					break;
					}
				
				System.out.println("Inicializando tranmissao novamente:");

				//uma vez que o pacote e recebido fora de ordem o laco e quebrado e a transmissao se inicia novamente
				//loop de transmissao
				
				while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){
					//Vetor para guardar parte dos bytes a serem enviados(mss - tamanho do segmento)
					byte[] filePacketBytes = new byte[MSS];						
					// Copia segmento de bytes de dados para vetor
					filePacketBytes = Arrays.copyOfRange(fileBytes, lastSent*MSS, lastSent*MSS + MSS);
						
					// Cria objeto rdtpacket (pacote de reliable data transfer - rdt)
					RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);
						
					// serializa pacote rdtpacket(objeto)
					byte[] sendData = Serializer.toBytes(rdtPacketObject);
						
					// cria pacote com especificacoes supracitadas(definidas), endereco de ip definido e porta 9876
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9888 );
						
					System.out.println("Enviado pacote com numero de sequencia " + lastSent +  ", tamanho: " + sendData.length + " bytes");
						
					//adiciona pacote a lista de envio
					sent.add(rdtPacketObject);
						
					// envia pacote com probabilidade previamente configurada de perda(neste caso sera 0)
					if(Math.random() >= 0){
						toReceiver.send(packet);
					}else{
						System.out.println("Perda de pacote com numero de sequencia " + lastSent);
					}
						
					//incrementa ultimo envio
					lastSent++;
						
					// vetor de bytes para ack enviado pelo receiver
					byte[] ackBytes = new byte[40];
					
						// cria pacote para ack
					DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
						
					try{
						//se algum ack nao foi recebido no tempo especifico(vai para o catch)
						toReceiver.setSoTimeout(TIMER);
							
						//recebe pacote
						toReceiver.receive(ack);
							
						// "deserializa" o rdtack(objeto) - transforma em objeto
						RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());
							
						System.out.println("ACK Recebido: " + ackObject.getPacket());
							
						// se este ack e do ultimo pacote, para o sender)
						if(ackObject.getPacket() == lastSeq){
							break;
						}
							
						waitingForAck = Math.max(waitingForAck, ackObject.getPacket());
							
	       				}catch(SocketTimeoutException e){
							//envia todos enviados que nao receberam packets ack
							
							for(int i = waitingForAck; i < lastSent; i++){
								
								//serializa o objeto rdtpacket
								byte[] sendData2 = Serializer.toBytes(sent.get(i));

								//cria o pacote
								DatagramPacket packet2 = new DatagramPacket(sendData2, sendData2.length, receiverAddress, 9888 );
								
								//envia com probabilidade estabelecida previamente
								if(Math.random() > 0){
									toReceiver.send(packet);
								}else{
									System.out.println("Pacote perdido, numero de sequencia: " + sent.get(i).getSeq());
								}

								System.out.println("Re-enviando pacote com numero de sequencia: " + sent.get(i).getSeq() +  " e tamanho: " + sendData.length + " bytes");
							}
						}
					
						
					}

				//}
			
		}
		if(controller==5) {
			System.out.println("Transmissão duplicada selecionada");
			System.out.print("Envie sua mensagem: ");
			msgin= br.readLine();
			//set lógica de transmisssao duplicada
			int probabilidadePerda=0;
			byte[] fileBytes = msgin.getBytes();
			System.out.println("Tamanho da mensagem: " + fileBytes.length + " bytes");

			// ultimo pacote -numero de sequencia
			int lastSeq = (int) Math.ceil( (double) fileBytes.length / MSS);
			System.out.println("Numero de pacotes para enviar: " + lastSeq);

			DatagramSocket toReceiver = new DatagramSocket();

			// Endereço do receiver
			InetAddress receiverAddress = InetAddress.getByName("localhost");
			
			//Lista de todos pacotes enviados
			ArrayList<RDTPacket> sent = new ArrayList<RDTPacket>();
			
			//loop de transmissao
			
			//while(true) {
				while(lastSent - waitingForAck < WINDOW_SIZE && lastSent < lastSeq){
					//Vetor para guardar parte dos bytes a serem enviados(mss - tamanho do segmento)
					byte[] filePacketBytes = new byte[MSS];
					
					// Copia segmento de bytes de dados para vetor
					filePacketBytes = Arrays.copyOfRange(fileBytes, lastSent*MSS, lastSent*MSS + MSS);
					
					// Cria objeto rdtpacket (pacote de reliable data transfer - rdt)
					RDTPacket rdtPacketObject = new RDTPacket(lastSent, filePacketBytes, (lastSent == lastSeq-1) ? true : false);
					
					// serializa pacote rdtpacket(objeto)
					byte[] sendData = Serializer.toBytes(rdtPacketObject);
					
					// cria pacote com especificacoes supracitadas(definidas), endereco de ip definido e porta 9876
					DatagramPacket packet = new DatagramPacket(sendData, sendData.length, receiverAddress, 9888 );
					//cria pacote duplicado para simular envio duplicado
					DatagramPacket packetDuplicado = new DatagramPacket(sendData, sendData.length, receiverAddress, 9888 );

					
					System.out.println("Enviado pacote com numero de sequencia " + lastSent +  ", tamanho: " + sendData.length + " bytes");
					
					//adiciona pacote a lista de envio
					sent.add(rdtPacketObject);
					
					// envia pacote com probabilidade previamente configurada de perda(neste caso sera 0)
					if(Math.random() >= probabilidadePerda){
						toReceiver.send(packet);
						toReceiver.send(packetDuplicado);
						System.out.println("Pacote duplicado enviado");
					}else{
						System.out.println("Perda de pacote com numero de sequencia " + lastSent);
					}
					
					//incrementa ultimo envio
					lastSent++;
					
					// vetor de bytes para ack enviado pelo receiver
					byte[] ackBytes = new byte[40];
					
					// cria pacote para ack
					DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);
					
					try{
						//se algum ack nao foi recebido no tempo especifico(vai para o catch)
						toReceiver.setSoTimeout(TIMER);
						
						//recebe pacote
						toReceiver.receive(ack);
						
						// "deserializa" o rdtack(objeto) - transforma em objeto
						RDTAck ackObject = (RDTAck) Serializer.toObject(ack.getData());
						
						System.out.println("ACK Recebido: " + ackObject.getPacket());
						
						// se este ack e do ultimo pacote, para o sender)
						if(ackObject.getPacket() == lastSeq){
							break;
						}
						
						waitingForAck = Math.max(waitingForAck, ackObject.getPacket());
						
					}catch(SocketTimeoutException e){
						//envia todos enviados que nao receberam packets ack
						
						for(int i = waitingForAck; i < lastSent; i++){
							
							//serializa o objeto rdtpacket
							byte[] sendData2 = Serializer.toBytes(sent.get(i));

							//cria o pacote
							DatagramPacket packet2 = new DatagramPacket(sendData2, sendData2.length, receiverAddress, 9888 );
							
							//envia com probabilidade estabelecida previamente
							if(Math.random() > probabilidadePerda){
								toReceiver.send(packet);
							}else{
								System.out.println("Pacote perdido, numero de sequencia: " + sent.get(i).getSeq());
							}

							System.out.println("Re-enviando pacote com numero de sequencia: " + sent.get(i).getSeq() +  " e tamanho: " + sendData.length + " bytes");
					}
				}		
			}
		}
	}
}