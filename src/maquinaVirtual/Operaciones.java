package maquinaVirtual;

import java.util.Random;
import java.util.Scanner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Operaciones {
	
	private static final int NEGATIVO = 0x80000000;  
	private static final int CERO = 0x40000000;  

    private final MemoriaBase memoria;
    private final Registros registros;
    private boolean jumpEjecutado; 
    TablaDescripSegmentosV2 tabla;
    private Archivos archivos;
    private int CS;

    public Operaciones(MemoriaBase memoria, Registros registros) {
        this.memoria = memoria;
        this.registros = registros;
        this.jumpEjecutado = false;
        this.tabla = null;
        this.archivos = null;
        this.CS = 0;
        
    }
    
    public Operaciones(MemoriaBase memoria, Registros registros, TablaDescripSegmentosV2 tabla,Archivos archivos,int CS) {
    	this.memoria = memoria;
    	this.registros = registros;
    	this.tabla = tabla;
    	this.archivos = archivos;
    	this.CS = CS;
    }
    
    public void ADD(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valorA    = obtenerValorOperando(tipoOpA, opA);
        int valorB    = obtenerValorOperando(tipoOpB, opB);
        int resultado = valorA + valorB;

        guardarValorEnDestino(tipoOpA, opA, resultado);
    }
    
    public void setCS(int CS) {
    	this.CS = CS;
    }
    public int tamanioOperandoReg(int operando) {
    	int codRegistro = (operando >>> 4) & 0x0F;
    	int sectorRegistro = (operando >>> 2) & 0x03;
    	
    	switch (sectorRegistro) {
        case 0b00:return 4; 
        case 0b01:return 1; 
        case 0b10:return 1;         
        case 0b11:return 2; 
        default:
            throw new IllegalArgumentException("Sector invalido: " + sectorRegistro);
    	}
    }

    
    public int tamanioOperando(byte tipoOp, int Op) {
    	
    	int tamanio = 0;
    	switch (tipoOp) {
    	case 0b11: tamanio = 4; break;
    	case 0b10: tamanio = 2; break;
    	case 0b01: tamanio = tamanioOperandoReg(Op);break;
    	}
    	
    	return tamanio;
    }

    public void MOV(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valorB = obtenerValorOperando(tipoOpB, opB);
        
        int srcBytes  = tamanioOperando(tipoOpB, opB);
        int destBytes = tamanioOperando(tipoOpA, opA);
        
        
        if (srcBytes < destBytes) {
            int maskSrc = (1 << (8 * srcBytes)) - 1;        
            int v       = valorB & maskSrc;
            int signBit = 1 << (8 * srcBytes - 1);        

            if ((v & signBit) != 0) {
                
                valorB = v | ~maskSrc;
            } else {
                
                valorB = v;
            }
        }
        
        guardarValorEnDestino(tipoOpA, opA, valorB);
    }

    public void SUB(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valorA    = obtenerValorOperando(tipoOpA, opA);
        int valorB    = obtenerValorOperando(tipoOpB, opB);
        int resultado = valorA - valorB;
        guardarValorEnDestino(tipoOpA, opA, resultado);
        
    }

    public void MUL(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int resultado = obtenerValorOperando(tipoOpA, opA)
                      * obtenerValorOperando(tipoOpB, opB);
        guardarValorEnDestino(tipoOpA, opA, resultado);
        
    }

    public void DIV(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int a = obtenerValorOperando(tipoOpA, opA);
        int b = obtenerValorOperando(tipoOpB, opB);
        if (b == 0) {
        	throw new IllegalArgumentException("Divide por 0");
        }
        registros.setAC(a % b);
        int cociente = a / b;
        guardarValorEnDestino(tipoOpA, opA, cociente);
        
    }


    public void CMP(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valA = obtenerValorOperando(tipoOpA, opA);
        int valB = obtenerValorOperando(tipoOpB, opB);


        valA = signExtend(valA, tipoOpA);
        valB = signExtend(valB, tipoOpB);
        
        

        int res = valA - valB;

        int cc = registros.getCC() & ~(CERO | NEGATIVO);
        if (res == 0)      cc |= CERO;
        else if (res < 0)  cc |= NEGATIVO;
        registros.setCC(cc);
    }

    private int signExtend(int valor, int tamanioBytes) {
        if (tamanioBytes >= 4) return valor;

        int mask = (1 << (8 * tamanioBytes)) - 1;
        int v = valor & mask;
        int signBit = 1 << (8 * tamanioBytes - 1);

        if ((v & signBit) != 0) {
           
            return v | ~mask;
        } else {
        
            return v;
        }
    }



    public void AND(byte tipoOpA, int opA, byte tipoOpB, int opB) {
       
        int valorA = obtenerValorOperando(tipoOpA, opA);
        int valorB = obtenerValorOperando(tipoOpB, opB);

        int resultado = valorA & valorB;

        guardarValorEnDestino(tipoOpA, opA, resultado);
    }
    
    
    
    
    
    public void OR(byte tipoOpA, int opA, byte tipoOpB, int opB) {

		int valorA = obtenerValorOperando(tipoOpA, opA);
		int valorB = obtenerValorOperando(tipoOpB, opB);

        int resultado = obtenerValorOperando(tipoOpA, opA)
                      | obtenerValorOperando(tipoOpB, opB);
		

        guardarValorEnDestino(tipoOpA, opA, resultado);
	}

    public void XOR(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int resultado = obtenerValorOperando(tipoOpA, opA)
                      ^ obtenerValorOperando(tipoOpB, opB);
        guardarValorEnDestino(tipoOpA, opA, resultado);
    }

    public void RND(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int limite   = obtenerValorOperando(tipoOpB, opB);
        int resultado = new Random().nextInt(limite);
        guardarValorEnDestino(tipoOpA, opA, resultado);
    }

    public void SWAP(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        if (tipoOpA == 0b10 || tipoOpB == 0b10)
            throw new IllegalArgumentException("SWAP no admite inmediatos");
        int valorA = obtenerValorOperando(tipoOpA, opA);
        int valorB = obtenerValorOperando(tipoOpB, opB);
        
        guardarValorEnDestino(tipoOpA, opA, valorB);
        guardarValorEnDestino(tipoOpB, opB, valorA);


    }

    public void SHL(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valor     = obtenerValorOperando(tipoOpA, opA);
        int desplaz   = obtenerValorOperando(tipoOpB, opB);
        int resultado = valor << desplaz;
        
        guardarValorEnDestino(tipoOpA, opA, resultado);
        
    }

    public void SHR(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valor     = obtenerValorOperando(tipoOpA, opA);
        int desplaz   = obtenerValorOperando(tipoOpB, opB);
        int resultado = valor >> desplaz;
        
        guardarValorEnDestino(tipoOpA, opA, resultado);
       
    }

    public void LDH(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int alto      = obtenerValorOperando(tipoOpA, opA);
        int bajo      = obtenerValorOperando(tipoOpB, opB);
        int resultado = (alto & 0x0000FFFF) | ((bajo & 0xFFFF) << 16);
        guardarValorEnDestino(tipoOpA, opA, resultado);
    }

    public void LDL(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int actual    = obtenerValorOperando(tipoOpA, opA);
        int bajo      = obtenerValorOperando(tipoOpB, opB);
        int resultado = (actual & 0xFFFF0000) | (bajo & 0xFFFF);
        guardarValorEnDestino(tipoOpA, opA, resultado);
    }
    
   
    
    public void JMP(byte tipoOpA, int opA) {
        
        int nuevoIP = obtenerValorOperando(tipoOpA, opA);

        
        registros.setRegistro("IP", CS +nuevoIP);
    }
    

	public void JZ(byte tipoOpA, int opA) {
	    int cc = registros.getCC();
	    if ((cc & CERO) != 0) {
	        int nuevoIP = obtenerValorOperando(tipoOpA, opA);
	        registros.setRegistro("IP", CS + nuevoIP);
	    }
	}

	public void JP(byte tipoOpA, int opA) {
	    int cc = registros.getCC();
	    boolean neg  = (cc & NEGATIVO) != 0;
	    boolean zero = (cc & CERO)     != 0;
	    if (!neg && !zero) {
	    	this.setJumpEjecutado(true);
	        int nuevoIP = obtenerValorOperando(tipoOpA, opA);
	        registros.setRegistro("IP", CS + nuevoIP);
	    }
	    else {
	    	this.setJumpEjecutado(false);
	    }
	}

	public void JN(byte tipoOpA, int opA) {
	    int cc = registros.getCC();
	    if ((cc & NEGATIVO) != 0) {
	    	this.setJumpEjecutado(true);
	        int nuevoIP = obtenerValorOperando(tipoOpA, opA);
	        registros.setRegistro("IP", CS +nuevoIP);
	    }
	    else {
	    	this.setJumpEjecutado(false);
	    }
	}

	public void JNZ(byte tipoOpA, int opA) {
		
	    int CC = registros.getCC();
	    if ((CC & CERO) == 0) {
	    	this.setJumpEjecutado(true);
	    	int nuevoIP = obtenerValorOperando(tipoOpA, opA);
	        this.registros.setIP(CS +nuevoIP);
	    }
	    else {
	    	this.setJumpEjecutado(false);
	    }
	}

	public void JNP(byte tipoOpA, int opA) {
	    int cc = registros.getCC();
	    boolean neg  = (cc & NEGATIVO) != 0;
	    boolean zero = (cc & CERO)     != 0;
	    if (neg || zero) {
	    	this.setJumpEjecutado(true);
	        int nuevoIP = obtenerValorOperando(tipoOpA, opA);
	        registros.setRegistro("IP", CS +nuevoIP);
	    }
	    else {
	    	this.setJumpEjecutado(false);
	    }
	}

	public void JNN(byte tipoOpA, int opA) {
	    int cc = registros.getCC();
	    if ((cc & NEGATIVO) == 0) {
	    	this.setJumpEjecutado(true);
	        int nuevoIP = obtenerValorOperando(tipoOpA, opA);
	        registros.setRegistro("IP", CS +nuevoIP);
	    }
	    else {
	    	this.setJumpEjecutado(false);
	    }
	}
	
    
	public void NOT(byte tipoOpA, int opA) {
	   
	    int valorOpA = obtenerValorOperando(tipoOpA, opA);
	    
	    int resultado = ~valorOpA;
	    
	    guardarValorEnDestino(tipoOpA, opA, resultado);

	    
	    int cc = 0;
	    if (resultado == 0) {
	        cc |= CERO;
	    }
	    if ((resultado & NEGATIVO) != 0) {
	        cc |= NEGATIVO;
	    }
	    registros.setCC(cc);
	}
	
	public void PUSH(byte tipoOpA, int opA) {
		registros.setSP(registros.getSP()-4);
		int limite = memoria.getDireccionFisica(registros.getSS());
		int SP = memoria.getDireccionFisica(registros.getSP());
		if(SP < limite) {
			throw new IndexOutOfBoundsException("Stack Overflow");
		}
		int val = obtenerValorOperando(tipoOpA,opA);
		int cantBytes = cantBytesLeidos(tipoOpA,opA);
		
		
		
		memoria.escribirPila(registros.getSP(),val,cantBytes);
	}

	public void POP(byte tipoOpA, int opA) {
		try {
			int val = memoria.leerPila(registros.getSP());
			guardarValorEnDestino(tipoOpA,opA,val);
			int SP = registros.getSP()+4;
			registros.setSP(SP);

		}
		catch(IndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Stack Underflow");			
		}
	}
	
	public void CALL(byte tipoOpA, int opA) {
		byte i=1;
		PUSH(i,0x50); 
		
		int offset = opA & 0x0000FFFF;
		registros.setIP(CS+offset);
		
	}

	
    public void RET() {
        try {
            int dirRetorno = memoria.leerPila(registros.getSP());
            registros.setSP(registros.getSP() + 4);
            registros.setIP(dirRetorno);
            this.setJumpEjecutado(true); 
        } catch(IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Stack Underflow en RET");
        }
    }


	
	public void SYS(byte tipoOpA, int opA) {
		
		
	    if (tipoOpA == 0b00) {
	        throw new IllegalArgumentException("SYS: falta el operando de modo");
	    }
	    int modo = obtenerValorOperando(tipoOpA, opA);

	    int dirLogicaBase = registros.getRegistro("EDX"); 
	    int ECX        = registros.getRegistro("ECX");
	    int EDX 	   = registros.getRegistro("EDX"); 
	    int CX	       = ECX & 0xFFFF;		 
	    int celdas     = ECX & 0xFF;          
	    int tamanio    = (ECX >> 8) & 0xFF;   
	    int formatoOperacion  = registros.getRegistro("EAX") & 0xFF; 


	    int cantBytesOperacion = celdas * tamanio;
	    


	    if (modo == 1) { 

            Scanner scanner = new Scanner(System.in);
            for (int i = 0; i < celdas; i++) {
                int dirLogica = memoria.agregarOffset(dirLogicaBase, i * tamanio);
                int dirFisica = memoria.getDireccionFisica(dirLogica);
                System.out.printf("[%04X]: ", dirFisica);

                String line = scanner.nextLine().trim();
                int valorInput;
                try {
                    switch (formatoOperacion) {
                        case 1: valorInput = Integer.parseInt(line, 10); break;
                        case 2: valorInput = line.length() > 0 ? (int) line.charAt(0) : 0; break;
                        case 4: valorInput = Integer.parseInt(line, 8); break;
                        case 8: valorInput = Integer.parseInt(line, 16); break;
                        case 10: valorInput = Integer.parseInt(line, 2); break;
                        default: valorInput = 0; break;
                    }
                } catch (NumberFormatException ex) {
                    System.err.println("Entrada invalida. Se usara 0.");
                    valorInput = 0;
                }
                memoria.escribirOperando(dirLogica, valorInput,tamanio);
            }
	    } else if (modo == 2) { 
	        
	    	for (int i = 0; i < celdas; i++) {
	        	int dato = memoria.leerOperando(dirLogicaBase, i*tamanio, tamanio); 
	            
	        	int dirLogicaCelda = memoria.agregarOffset(dirLogicaBase, i*tamanio);
	        	int dirFisica = memoria.getDireccionFisica(dirLogicaCelda);
	            
	        	
	        	String salidaFormateada = formarStringSalida(formatoOperacion, dato, tamanio); 	            
	        	
	        	System.out.println("[" + String.format("%04X", dirFisica) + "]: " +salidaFormateada);
	        }
	    }	  
	    else if(modo == 3) {
	    	Scanner scanner = new Scanner(System.in);
	    	System.out.println("SYS 3: ");	    	
	    	String input = scanner.nextLine();
	    	int maxChars;
	    	
	    	if(CX <= -1) {
	    		maxChars = input.length();
	    	}
	    	else {
	    		maxChars = Math.min(CX, input.length());
	    	}
	    	
	    	for(int i=0; i<maxChars;i++) {
	    		memoria.escribirByteLogica(EDX + i, (byte)input.charAt(i));
	    		System.out.println((EDX+i) + " "+ (byte)input.charAt(i));
	    	}
	    		
	    }
	    else if(modo == 4) {
	    	StringBuilder str = new StringBuilder();
	    	
	    	int offset=0;
	    	while(memoria.leerByteLogica(EDX +offset) != 0) {
	    		str.append((char)memoria.leerByteLogica(EDX + offset));	
	    		offset++;
	    	}
	    	System.out.println(str);
	    }
	    else if(modo == 7) {
	    	clearScreen();
	    }
	    else if (modo == 0xF) { 
	    	try {	    		
	    		if(archivos.tieneVMI()) {
	    			archivos.guardarArchivoVMI(this.registros, this.memoria, this.tabla);	    			
	    		}
	    	}
	    	catch(IOException e) {
	    		System.out.println(e);
	    	}
	    }
	    else {
	        System.err.println("SYS: modo no soportado (" + modo + ")");
	    }
	    
	}
	
	private void clearScreen() {
		for(int i=0;i<50;i++) {
			System.out.println();
		}
	}
	public static String formarStringSalida(int formatoOperacion, int dato, int tamanioBytes) {

	    List<String> salidas = new ArrayList<>();

	    
	    
	    if ((formatoOperacion & 16) != 0) {
	        salidas.add("0b" + Integer.toBinaryString(dato));
	    }
	    
	    if ((formatoOperacion & 8) != 0) {
	        salidas.add("0x" + Integer.toHexString(dato).toUpperCase());
	    }
	   
	    if ((formatoOperacion & 4) != 0) {
	        salidas.add("0o" + Integer.toOctalString(dato));
	    }
	    
	    if ((formatoOperacion & 2) != 0) {
	        char c = (char) dato;
	        String ch = (c >= 32 && c <= 126) ? String.valueOf(c) : ".";
	        salidas.add(ch);
	    }
	    
	    if ((formatoOperacion & 1) != 0) {
	        salidas.add(String.valueOf(dato));
	    }

	    if (salidas.isEmpty()) {
	        return "formato no soportado";
	    }
	    
	    return String.join("  ", salidas);
	}
	
	private int obtenerValorOperando(byte tipoOp, int operando) {
        switch (tipoOp) {
            case 0b01: 
            	return registros.leerSectorRegistro(operando);
            case 0b10: 
                return operando;
            case 0b11:
            	int codRegistro = operando >> 4 & 0xF;
            	String nombreReg = registros.getNombreRegistro(codRegistro);
            	int dirLogicaEnRegistro = registros.getRegistro(nombreReg);

            	
            	short offsetRaw = (short) ((operando >> 8) & 0xFFFF);
            	int offsetAdicional = offsetRaw;

				int dirLogicaMasOffset = memoria.agregarOffset(dirLogicaEnRegistro, offsetAdicional);
				
				
                int tamanoCelda = operando & 0x3;
                int cantBytes;
                switch (tamanoCelda) {
                    case 0b00: cantBytes = 4; break; 
                    case 0b10: cantBytes = 2; break; 
                    case 0b11: cantBytes = 1; break; 
                    default: cantBytes = 4; break;
                }
                
				int BytesLeidosDeMemoria = memoria.leerMemoria(dirLogicaMasOffset, cantBytes);

				return BytesLeidosDeMemoria;
			
        }
		return 0;
    }

    private void guardarValorEnDestino(byte tipoDestino, int operandoDestino, int valor) {

    	int cantBytesOperacion = 0;
        switch (tipoDestino) {
            case 0b01:
                cantBytesOperacion = registros.escribirSectorRegistro(operandoDestino, valor);
                break;
            case 0b11:
                
                int tamanoCelda = operandoDestino & 0b11;
                int cantBytes;
                switch (tamanoCelda) {
                    case 0b00: cantBytes = 4; break; 
                    case 0b10: cantBytes = 2; break; 
                    case 0b11: cantBytes = 1; break; 
                    default: cantBytes = 4; break;
                }
                
                
                int codRegistro = (operandoDestino >> 4) & 0xF; 
                String nombreReg = registros.getNombreRegistro(codRegistro);
                int punteroAlmacenado = registros.getRegistro(nombreReg);

                
                short offsetRaw = (short) ((operandoDestino >> 8) & 0xFFFF);
                int offsetExtra = offsetRaw;

                int dirLogicaFinal = (punteroAlmacenado & 0xFFFF0000) | (((punteroAlmacenado & 0xFFFF) + offsetExtra) & 0xFFFF);

                memoria.escribirOperando(dirLogicaFinal, valor, cantBytes); 
                cantBytesOperacion = cantBytes;
                break;
            default:
                System.out.println("Tipo de destino no soportado: " + tipoDestino);
                break;
        }

        this.registros.modificarCC(valor, cantBytesOperacion);
    }
	public boolean isJumpEjecutado() {
		return jumpEjecutado;
	}

	public void setJumpEjecutado(boolean jumpEjecutado) {
		this.jumpEjecutado = jumpEjecutado;
	}
	private int cantBytesLeidos(byte tipoOp, int operando) {
	   
	    switch (tipoOp) {
	        case 0b01:  
	            int sector = (operando >>> 2) & 0x03;
	            int cant = 0;
	            switch (sector) {
	                case 0b00: cant = 4; break;  
	                case 0b01: cant = 1; break;  
	                case 0b10: cant = 1; break;  
	                case 0b11: cant = 2; break;  
	                default: throw new IllegalArgumentException("Sector inválido: " + sector);
	            }
	            return cant;
	        case 0b10:  
	            return 2;
	        case 0b11:  
	            return 4 - (operando & 0x3);
	        default:
	            return 0;
	    }
	}

    private int cantidadBytesOperando(byte tipoOperando) {
        switch (tipoOperando) {
            case 0b00: return 0;  
            case 0b01: return 1;  
            case 0b10: return 2;  
            case 0b11: return 3;  
            default: return 0;    
        }
    }
    
    
}    
