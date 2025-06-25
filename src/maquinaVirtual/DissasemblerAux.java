package maquinaVirtual;

public class DissasemblerAux {

    private Registros registros;
    private MemoriaBase memoria;
    
    public DissasemblerAux(Registros registros, MemoriaBase memoria) {
        this.registros = registros;
        this.memoria = memoria;
    }
	
    public int decodificarInstruccion(byte primerByte) {
    	
        byte codOperacion = (byte) (primerByte & 0x1F);
        int bytesYaLeidosInstruccion = 1; 

        byte tipoOpA = 0, tipoOpB = 0;int valorOpA = 0, valorOpB = 0;int cantBytesOpA = 0, cantBytesOpB = 0;
        String instHexa = ""; String lineaDissasembler = "";
        if (codOperacion <= 0x09) {   
            tipoOpA = (byte) ((primerByte >> 6) & 0x03); 
            
            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion); 
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;
            
            
            
            instHexa = DissasemblerAux.concatenarInstHexa(primerByte, valorOpA, null);
            lineaDissasembler = String.format("%s %s",
            		DissasemblerAux.codOpAMnemonico(codOperacion),
            		DissasemblerAux.decodificarOp(registros, tipoOpA, valorOpA));

                    
        } 

        else if (codOperacion >= 0x10 && codOperacion <= 0x1D) { 
        	tipoOpA = (byte) ((primerByte & 0x30) >> 4); 
            tipoOpB = (byte) ((primerByte & 0xC0) >> 6);
            
            valorOpB = obtenerOpEnMemoria(tipoOpB, bytesYaLeidosInstruccion);
            cantBytesOpB = cantidadBytesOperando(tipoOpB);
            bytesYaLeidosInstruccion += cantBytesOpB;
            
            
            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion);
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
	        bytesYaLeidosInstruccion += cantBytesOpA;

	        
            instHexa = DissasemblerAux.concatenarInstHexa(primerByte, valorOpB, valorOpA);
            lineaDissasembler = String.format("%s %s, %s",
            		DissasemblerAux.codOpAMnemonico(codOperacion),
            		DissasemblerAux.decodificarOp(registros, tipoOpA, valorOpA),
            		DissasemblerAux.decodificarOp(registros, tipoOpB, valorOpB));
                
            
        } else if (codOperacion == 0x0F) { 
           instHexa = "0F";
           lineaDissasembler = "STOP";
          
        } else {
        	bytesYaLeidosInstruccion = -1; 
        }
        
        
        System.out.printf("[%04X] %-20s | %s\n", registros.getIP(), instHexa, lineaDissasembler);
       
        return bytesYaLeidosInstruccion;

    }
    
    public int obtenerOpEnMemoria(byte tipoOperando, int bytesYaLeidosInstruccion) {
        
        
    	int comienzoInstruccion = registros.getIP(); 

    	switch (tipoOperando) {
            case 0b01: 
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 1);
            case 0b10: 
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 2);
            case 0b11: 
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 3);
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
    
    public static String codOpAMnemonico(byte opcode) {
        switch (opcode) {
            case 0x0:  return "SYS";
            case 0x1:  return "JMP";
            case 0x2:  return "JZ";
            case 0x3:  return "JP";
            case 0x4:  return "JN";
            case 0x5:  return "JNZ";
            case 0x6:  return "JNP";
            case 0x7:  return "JNN";
            case 0x8:  return "NOT";
            case 0x0F: return "STOP";
            case 0x10: return "MOV";
            case 0x11: return "ADD";
            case 0x12: return "SUB";
            case 0x13: return "SWAP";
            case 0x14: return "MUL";
            case 0x15: return "DIV";
            case 0x16: return "CMP";
            case 0x17: return "SHL";
            case 0x18: return "SHR";
            case 0x19: return "AND";
            case 0x1A: return "OR";
            case 0x1B: return "XOR";
            case 0x1C: return "LDL";
            case 0x1D: return "LDH";
            case 0x1E: return "RND";
            default: return "UNK";
        }
    }

    
    public static String decodificarOp(Registros registros, byte tipoOp, int operando) {
        switch (tipoOp) {
        
            case 0b01: 
            		   
            	int aux = operando >> 4 & 0xF;
            	String nombreRegCompleto = registros.getNombreRegistro(aux);
            	int sector = (operando >> 2) & 0x3;
                if (sector == 0) {
                    return nombreRegCompleto;
                }
                char medio = nombreRegCompleto.charAt(1); 
                String[] SUFIJOS = { "", "L", "H", "X" };
                return medio + SUFIJOS[sector]; 
            
            case 0b10: 
                short inmediatoConSigno = (short) operando;     
                return String.valueOf((int) inmediatoConSigno);
            
            case 0b11:
                int codReg = (operando >> 4) & 0xF;
                String nombreReg = registros.getNombreRegistro(codReg);
                byte offsetRaw = (byte) ((operando >> 8) & 0xFF);   
                int offsetConSigno = offsetRaw;                    
                
                if (offsetConSigno == 0) {
                    return "[" + nombreReg + "]";
                } else {
                    return "[" + nombreReg + " + " + offsetConSigno + "]";
                }
            
            default: return "";
        }
    }

    
    public static String concatenarInstHexa(byte primerByte, int operandoA, Integer operandoB) {
        StringBuilder sb = new StringBuilder();

        
        sb.append(String.format("%02X ", primerByte));
        sb.append(String.format("%04X", operandoA));

        
        if (operandoB != null) {
            sb.append(" ");
            sb.append(String.format("%04X", operandoB));
        }

        return sb.toString();
    }
}