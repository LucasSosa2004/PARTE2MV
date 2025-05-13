package maquinaVirtual;

public class UnidadAritmeticoLogica {
	
    private Registros registros;
    private MemoriaBase memoria;
    private Operaciones operaciones;
    private final boolean testMode;
    
    public UnidadAritmeticoLogica(Registros registros, MemoriaBase memoria, boolean testMode) {
        this.registros = registros;
        this.memoria = memoria;
        this.operaciones = new Operaciones(memoria, registros);
        this.testMode = testMode;  // Bandera de modo disassembler
    }
    

public int ejecutarInstruccion(byte primerByte) {
    	
        byte codOperacion = (byte) (primerByte & 0x1F);
        int bytesYaLeidosInstruccion = 1;  
        byte tipoOpA = 0, tipoOpB = 0;int valorOpA = 0, valorOpB = 0;int cantBytesOpA = 0, cantBytesOpB = 0;
        String instHexa = ""; String lineaDissasembler = "";
        if (codOperacion >= 0 && codOperacion <= 0x09) {  // Instrucciones de un operando 
            tipoOpA = (byte) ((primerByte >> 6) & 0x03); //aplico mascara para quedarme con los primeros dos bits

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion); 
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;
            
            // Si estamos en modo TestMode, armamos la representacion textual.
            if (testMode) {
            	instHexa = DissasemblerAux.concatenarInstHexa(primerByte, valorOpA, null);
            	lineaDissasembler = String.format("%s %s",
            			DissasemblerAux.codOpAMnemonico(codOperacion),
            			DissasemblerAux.decodificarOp(registros, tipoOpA, valorOpA));
            }
            	
            
                    
        } 

        else if (codOperacion >= 0x10 && codOperacion <= 0x1E) { //Dos operandos
        	tipoOpB = (byte) ((primerByte & 0xC0) >> 6);
        	tipoOpA = (byte) ((primerByte & 0x30) >> 4); 
            
            valorOpB = obtenerOpEnMemoria(tipoOpB, bytesYaLeidosInstruccion);
            cantBytesOpB = cantidadBytesOperando(tipoOpB);
            bytesYaLeidosInstruccion += cantBytesOpB;
            
            // Se obtiene operando A, que se encuentra justo despues del operando B
            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion);
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
	        bytesYaLeidosInstruccion += cantBytesOpA;

	        
            if (testMode) {
            	instHexa = DissasemblerAux.concatenarInstHexa(primerByte, valorOpB, valorOpA);
            	lineaDissasembler = String.format("%s %s, %s",
            			DissasemblerAux.codOpAMnemonico(codOperacion),
            			DissasemblerAux.decodificarOp(registros, tipoOpA, valorOpA),
            			DissasemblerAux.decodificarOp(registros, tipoOpB, valorOpB));
            }
                
        } 
        // Caso de instruccion STOP u otra no reconocida.
        else if (codOperacion == 0x0F) {
            if (testMode) {
            	instHexa = "0F";
            	lineaDissasembler = "STOP";
            }
            
            bytesYaLeidosInstruccion = -1; //o solo para cuando es ejecucion?
        } else {
        	bytesYaLeidosInstruccion = -1; // instruccion invalida -> el codigo de operacion no existe
        }
        
        if (bytesYaLeidosInstruccion > 0 && !this.isJumpEjecutado()) { 
            this.registros.modificaIP(bytesYaLeidosInstruccion);                        
        } else { //Si hubo salto, el propio salto modifica el valor del IP
        	this.setJumpEjecutado(false);
        }
        
        if ((codOperacion >= 0 && codOperacion <= 0x09)) {
        	ejecutarOperacionUnOperando(codOperacion, tipoOpA, valorOpA);
        }
        else if (codOperacion >= 0x10 && codOperacion <= 0x1E) {
        	ejecutarOperacionDosOperandos(codOperacion, tipoOpA, valorOpA, tipoOpB, valorOpB);
        }
        
        // Si estamos en modo disassembler, imprimimos la instruccion sin ejecutarla.
        if (testMode) {
            System.out.printf("[%04X] %-20s | %s\n", registros.getIP(), instHexa, lineaDissasembler);
        }
        
        
        return bytesYaLeidosInstruccion;

    }
	
	
    public int obtenerOpEnMemoria(byte tipoOperando, int bytesYaLeidosInstruccion) {
        // Se calcula la posicion de inicio para leer el operando. Caso 2 operandos, bytesYaLeidos tendra un valor mayor
        
    	int comienzoInstruccion = registros.getIP(); //La idea es usar dir logicas solamente

    	switch (tipoOperando) {
            case 0b01: // Operando de registro (1 byte)
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 1);
            case 0b10: // Operando inmediato (2 bytes)
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 2);
            case 0b11: // Operando de memoria (3 bytes)
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 3);
            default:
            	return 0;
        }
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
	
	
    // Metodo auxiliar para ejecutar instrucciones de un operando.
    private void ejecutarOperacionUnOperando(byte codOperacion, byte tipoOp, int valorOp) {
        switch (codOperacion) {
            case 0: operaciones.SYS(tipoOp, valorOp); break;
            case 1: operaciones.JMP(tipoOp, valorOp); break;
            case 2: operaciones.JZ(tipoOp, valorOp); break;
            case 3: operaciones.JP(tipoOp, valorOp); break;
            case 4: operaciones.JN(tipoOp, valorOp); break;
            case 5: operaciones.JNZ(tipoOp, valorOp); break;
            case 6: operaciones.JNP(tipoOp, valorOp); break;
            case 7: operaciones.JNN(tipoOp, valorOp); break;
            case 8: operaciones.NOT(tipoOp, valorOp); break;
            case 11: operaciones.PUSH(tipoOp, valorOp);break;
            case 12: operaciones.POP(tipoOp,valorOp); break;
            default: break;
        }
    }
    
    // Metodo auxiliar para ejecutar instrucciones de dos operandos.
    private void ejecutarOperacionDosOperandos(byte codOperacion, byte tipoOpA, int valorOpA, byte tipoOpB, int valorOpB) {
    	switch (codOperacion) {
            case 0x10: operaciones.MOV(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x11: operaciones.ADD(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x12: operaciones.SUB(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x13: operaciones.SWAP(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x14: operaciones.MUL(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x15: operaciones.DIV(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x16: operaciones.CMP(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x17: operaciones.SHL(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x18: operaciones.SHR(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x19: operaciones.AND(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x1A: operaciones.OR(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x1B: operaciones.XOR(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x1C: operaciones.LDL(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x1D: operaciones.LDH(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            case 0x1E: operaciones.RND(tipoOpA, valorOpA, tipoOpB, valorOpB); break;
            default: break;
        }
    }
    
	public boolean isJumpEjecutado() {
		return operaciones.isJumpEjecutado();
	}
	
	public void setJumpEjecutado(boolean jumpEjecutado) {
		operaciones.setJumpEjecutado(jumpEjecutado);
	}


}





