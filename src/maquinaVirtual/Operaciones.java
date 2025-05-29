package maquinaVirtual;

import java.util.Random;
import java.util.Scanner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Operaciones {
	
	private static final int NEGATIVO = 0x80000000;  // bit 32 = Negativo
	private static final int CERO = 0x40000000;  // bit 31 = Cero

    private final MemoriaBase memoria;
    private final Registros registros;
    private boolean jumpEjecutado; 
    TablaDescripSegmentosV2 tabla;
    private Archivos archivos;
    private final int CS;

    public Operaciones(MemoriaBase memoria, Registros registros) {
        this.memoria = memoria;
        this.registros = registros;
        this.jumpEjecutado = false;
        this.tabla = null;
        this.archivos = null;
        this.CS = 0;
        
    }
    
    public Operaciones(MemoriaBase memoria, Registros registros, TablaDescripSegmentosV2 tabla,Archivos archivos) {
    	this.memoria = memoria;
    	this.registros = registros;
    	this.tabla = tabla;
    	this.archivos = archivos;
    	this.CS = tabla.getIndice("CS")<<16;
    }
    
    public void ADD(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valorA    = obtenerValorOperando(tipoOpA, opA);
        int valorB    = obtenerValorOperando(tipoOpB, opB);
        int resultado = valorA + valorB;
        // resultado tiene el tamano de tipoOpA
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
    }
    
    public int tamanioOperandoReg(int operando) {
    	int codRegistro = (operando >>> 4) & 0x0F;
    	int sectorRegistro = (operando >>> 2) & 0x03;
    	
    	switch (sectorRegistro) {
        case 0b00:return 4; // Entero completo (32 bits)
        case 0b01:return 1; // Byte mas bajo (ej: AL)  bits 0..7
        case 0b10:return 1; // Segundo byte mas bajo (ej: AH)  bits 8..15            
        case 0b11:return 2; // Dos bytes bajos (ej: AX)  bits 0..15
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
        
        // Si la fuente es mas pequena que el destino, hacemos sign-extension
        if (srcBytes < destBytes) {
            int maskSrc = (1 << (8 * srcBytes)) - 1;        // bits bajos de la fuente
            int v       = valorB & maskSrc;
            int signBit = 1 << (8 * srcBytes - 1);         // bit de signo en la fuente

            if ((v & signBit) != 0) {
                // valor negativo en N bytes: rellenar bits altos a 1
                valorB = v | ~maskSrc;
            } else {
                // positivo: alto queda en 0
                valorB = v;
            }
        }
        /*
        if(srcBytes<destBytes) {
        	signExtend(valorB,srcBytes);
        }
        }*/

        // propagamos el tamano del operando fuente
        guardarValorEnDestino(tipoOpA, opA, valorB);
    }

    public void SUB(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valorA    = obtenerValorOperando(tipoOpA, opA);
        int valorB    = obtenerValorOperando(tipoOpB, opB);
        int resultado = valorA - valorB;
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
    }

    public void MUL(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int resultado = obtenerValorOperando(tipoOpA, opA)
                      * obtenerValorOperando(tipoOpB, opB);
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
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
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, cociente);
    }

/*    public void CMP(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valA = obtenerValorOperando(tipoOpA, opA);
        int valB = obtenerValorOperando(tipoOpB, opB);
        int res = valA - valB;
        System.out.println(valA + " a - b " + valB);
        System.out.println("CMP" + res);
        int cc = registros.getCC() & ~(CERO | NEGATIVO);
        if (res == 0)      cc |= CERO;
        else if (res < 0)  cc |= NEGATIVO;
        registros.setCC(cc);
    }*/
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
            // Negativo: rellenamos con 1s
            return v | ~mask;
        } else {
            // Positivo: queda igual
            return v;
        }
    }



    public void AND(byte tipoOpA, int opA, byte tipoOpB, int opB) {
       
        int valorA = obtenerValorOperando(tipoOpA, opA);
        int valorB = obtenerValorOperando(tipoOpB, opB);

        int resultado = valorA & valorB;

        guardarValorEnDestino(tipoOpA, opA, resultado);
    }
    
    
    
    
    /*public void AND(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int resultado = obtenerValorOperando(tipoOpA, opA)
                      & obtenerValorOperando(tipoOpB, opB);
        
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
    }*/

    public void OR(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int resultado = obtenerValorOperando(tipoOpA, opA)
                      | obtenerValorOperando(tipoOpB, opB);
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
    }

    public void XOR(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int resultado = obtenerValorOperando(tipoOpA, opA)
                      ^ obtenerValorOperando(tipoOpB, opB);
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
    }

    public void RND(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int limite   = obtenerValorOperando(tipoOpB, opB);
        int resultado = new Random().nextInt(limite);
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
    }

    public void SWAP(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        if (tipoOpA == 0b10 || tipoOpB == 0b10)
            throw new IllegalArgumentException("SWAP no admite inmediatos");
        int valorA = obtenerValorOperando(tipoOpA, opA);
        int valorB = obtenerValorOperando(tipoOpB, opB);
        // ojo al orden: el tipoFuente de cada write es el tipo del valor que escribes
        guardarValorEnDestino(tipoOpA, opA, valorB);
        guardarValorEnDestino(tipoOpB, opB, valorA);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, valorB);
        //guardarValorEnDestino(tipoOpB, opB, tipoOpA, opA, valorA);

    }

    public void SHL(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valor     = obtenerValorOperando(tipoOpA, opA);
        int desplaz   = obtenerValorOperando(tipoOpB, opB);
        int resultado = valor << desplaz;
        
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
        // actualizar CC segun resultado
    }

    public void SHR(byte tipoOpA, int opA, byte tipoOpB, int opB) {
        int valor     = obtenerValorOperando(tipoOpA, opA);
        int desplaz   = obtenerValorOperando(tipoOpB, opB);
        int resultado = valor >> desplaz;
        
        guardarValorEnDestino(tipoOpA, opA, resultado);
        //guardarValorEnDestino(tipoOpA, opA, tipoOpB, opB, resultado);
        // actualizar CC
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
    
    // Operaciones un operando
    
    public void JMP(byte tipoOpA, int opA) {
        // Obtiene la direccion objetivo (ya sea literal, desde un registro o desde memoria)
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
	    // 1) Lees el operando
	    int valorOpA = obtenerValorOperando(tipoOpA, opA);
	    // 2) Calculas la negacion bit a bit
	    int resultado = ~valorOpA;
	    // 3) Guardas usando tipoFuente = tipoOpA
	    guardarValorEnDestino(tipoOpA, opA, resultado);

	    // 4) Actualizas flags CC
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
		//System.out.println("PUSH"+ Integer.toHexString(val));

		//memoria.imprimirMemoria(tabla.getSegmento("SS").getBase()+tabla.getSegmento("SS").getTamanio()-50,52);
		memoria.escribirPila(registros.getSP(),val);
	}

	public void POP(byte tipoOpA, int opA) {
		try {
			int val = memoria.leerPila(registros.getSP());
			guardarValorEnDestino(tipoOpA,opA,val);
			int SP = registros.getSP()+4;
			registros.setSP(SP);
			System.out.println("POP"+ Integer.toHexString(val));
			//System.out.println("POP "+ val);
			//memoria.imprimirMemoria(tabla.getSegmento("SS").getLimite()-40,41);
		}
		catch(IndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Stack Underflow");			
		}
	}
	
	public void CALL(byte tipoOpA, int opA) {
		byte i=1;//no deja poner cte abajo
		PUSH(i,0x50); //RL IP SE ESTA HACIENDO -1
		//System.out.println("Call"+ Integer.toHexString(registros.getIP()));		
		JMP(tipoOpA,opA);
	}
	
	public void RET() {
		byte i=1;
		POP(i,0x50);
	}
	
	public void SYS(byte tipoOpA, int opA) {
		//System.out.println("entro al sys");
		
	    if (tipoOpA == 0b00) {
	        throw new IllegalArgumentException("SYS: falta el operando de modo");
	    }
	    int modo = obtenerValorOperando(tipoOpA, opA);

	    int dirLogicaBase = registros.getRegistro("EDX"); // (ya apunta al DS + offset)
	    int ECX        = registros.getRegistro("ECX");
	    int EDX 	   = registros.getRegistro("EDX"); 
	    int CX	       = ECX & 0xFFFF;		  // CX
	    int celdas     = ECX & 0xFF;          // CL 
	    int tamanio    = (ECX >> 8) & 0xFF;   // CH 
	    int formatoOperacion  = registros.getRegistro("EAX") & 0xFF; //AL (1=hex, 2=bin, 4=oct, resto=dec)


	    int cantBytesOperacion = celdas * tamanio;
	    if (modo == 1) { //Read

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
                memoria.escribirOperando(dirLogica, valorInput);
            }
	    } else if (modo == 2) { //WRITE
	        
	    	for (int i = 0; i < celdas; i++) {
	        	int dato = memoria.leerOperando(dirLogicaBase, i*tamanio, tamanio); //i*tamanio = cant bytes que ya lei
	            //OBTENER LA DIR FISICA DE LA CELDA QUE SE LEE
	        	int dirLogicaCelda = memoria.agregarOffset(dirLogicaBase, i*tamanio);
	        	int dirFisica = memoria.getDireccionFisica(dirLogicaCelda);
	            
	        	//hay q indicar el tamanio del dato que se leyo (asi se muestra con el signo que corresponde y formato apropiado)
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
	    	}
	    		
	    }
	    else if(modo == 4) {
	    	StringBuilder str = new StringBuilder();
	    	
	    	int offset=0;
	    	while(memoria.leerByteLogica(EDX +offset) != 0) {
	    		str.append((char)memoria.leerByteLogica(EDX + offset));	
	    		offset++;
	    	}
	    	str.append(0);
	    	System.out.println(str);
	    }
	    else if(modo == 7) {
	    	clearScreen();
	    }
	    else if (modo == 0xF) { //breakpoint
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
	    // 0) Calcular cuantos bytes realmente importan:
	    //    buscamos el primer 1 de los 32 bits; eso define effectiveBytes.
	    /*
		int effectiveBytes;
	    if (datoRaw == 0) {
	        effectiveBytes = 1;
	    } else {
	        int bitLen = 32 - Integer.numberOfLeadingZeros(datoRaw);
	        effectiveBytes = ((bitLen - 1) / 8) + 1;
	    }
	    // nunca pasarnos de lo que realmente lei
	    effectiveBytes = Math.min(effectiveBytes, tamanioBytes);

	    // 1) Sign-extend segun effectiveBytes
	    int datoSigned;
	    switch (effectiveBytes) {
	        case 1:
	            datoSigned = (byte) datoRaw;    // extiende signo de 8 bits
	            break;
	        case 2:
	            datoSigned = (short) datoRaw;   // extiende signo de 16 bits
	            break;
	        case 3:
	            // extiende signo de 24 bits
	            if ((datoRaw & 0x800000) != 0) {
	                datoSigned = datoRaw | 0xFF000000;
	            } else {
	                datoSigned = datoRaw & 0x00FFFFFF;
	            }
	            break;
	        default:
	            // 4 bytes o mas: datoRaw ya esta en 32 bits con signo
	            datoSigned = datoRaw;
	    }

	    // 2) Mascara unsigned para imprimir bin/hex/oct
	    int maskUnsigned = (effectiveBytes >= 4)
	            ? 0xFFFFFFFF
	            : (1 << (8 * effectiveBytes)) - 1;
	    int datoUnsigned = datoRaw & maskUnsigned;
	    */
	    List<String> salidas = new ArrayList<>();

	    // 3) Formateos segun flags de formato
	    // Binario (bit 4 = 16)
	    if ((formatoOperacion & 16) != 0) {
	        salidas.add("0b" + Integer.toBinaryString(dato));
	    }
	    // Hexadecimal (bit 3 = 8)
	    if ((formatoOperacion & 8) != 0) {
	        salidas.add("0x" + Integer.toHexString(dato).toUpperCase());
	    }
	    // Octal (bit 2 = 4)
	    if ((formatoOperacion & 4) != 0) {
	        salidas.add("0o" + Integer.toOctalString(dato));
	    }
	    // ASCII (bit 1 = 2)
	    if ((formatoOperacion & 2) != 0) {
	        char c = (char) dato;
	        String ch = (c >= 32 && c <= 126) ? String.valueOf(c) : ".";
	        salidas.add(ch);
	    }
	    // Decimal con signo (bit 0 = 1)
	    if ((formatoOperacion & 1) != 0) {
	        salidas.add(String.valueOf(dato));
	    }

	    if (salidas.isEmpty()) {
	        return "formato no soportado";
	    }
	    // Doble espacio como separador
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
        
            	int offsetAdicional = operando >> 8 & 0xFF;
            	//int dirLogicaMasOffset = (dirLogicaEnRegistro & 0xFFFF0000) | (((dirLogicaEnRegistro & 0xFFFF) + offsetAdicional) & 0xFFFF);
            	int dirLogicaMasOffset = dirLogicaEnRegistro + offsetAdicional;
	    		int cantBytes = operando & 0x3;
            	cantBytes = 4 - cantBytes;
                return memoria.leerMemoria(dirLogicaMasOffset, cantBytes); //si es v1 cantBytes se convierte en 4
            default: return 0;
        }
    }
    private void guardarValorEnDestino(byte tipoDestino, int operandoDestino, int valor) {

    	int cantBytesOperacion = 0;
        switch (tipoDestino) {
            case 0b01:
                cantBytesOperacion = registros.escribirSectorRegistro(operandoDestino, valor);
                break;
            case 0b11:
                int codRegistro = operandoDestino >> 4 & 0xF; //Registro que tiene el puntero
                String nombreReg = registros.getNombreRegistro(codRegistro);
                int punteroAlmacenado = registros.getRegistro(nombreReg);
                int offsetExtra = operandoDestino >> 8 & 0xFF;
		        int cantBytes = operandoDestino & 0x3;
		        cantBytes = 4 - cantBytes;
		        
                int dirLogicaFinal = (punteroAlmacenado & 0xFFFF0000) | (((punteroAlmacenado & 0xFFFF) + offsetExtra) & 0xFFFF);
                memoria.escribirOperando(dirLogicaFinal, valor,cantBytes); 
                cantBytesOperacion = 4;
                break;
            default:
                System.out.println("Tipo de destino no soportado.");
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
	
    private int cantidadBytesOperando(byte tipoOperando) {
        switch (tipoOperando) {
            case 0b00: return 0;  // Sin operando
            case 0b01: return 1;  // Registro (1 byte)
            case 0b10: return 2;  // Inmediato (2 bytes)
            case 0b11: return 3;  // Memoria (3 bytes)
            default: return 0;    // Por defecto
        }
    }
    
    
}    
