package maquinaVirtual;

import java.util.List;

public class UnidadAritmeticoLogica {
	
    private Registros registros;
    private MemoriaBase memoria;
    private Operaciones operaciones;
    private final boolean testMode;
    private boolean breakPointAnterior;

    
    
    public UnidadAritmeticoLogica(Registros registros, MemoriaBase memoria, boolean testMode) {
    	this.registros = registros;
    	this.memoria = memoria;
    	this.operaciones = new Operaciones(memoria, registros);
    	this.testMode = testMode;  
    }

    public UnidadAritmeticoLogica(Registros registros, MemoriaBase memoria, TablaDescripSegmentosV2 tabla,Archivos archivos, boolean testMode) {
        this.registros = registros;
        this.memoria = memoria;
        int CS = tabla.getIndice("CS")<<16;
        this.operaciones = new Operaciones(memoria, registros, tabla,archivos,CS);
        this.testMode = testMode;  
    }	
    

    public int ejecutarInstruccion(byte primerByte) {
    	
    	
        byte codOperacion = (byte) (primerByte & 0x1F);
        int bytesYaLeidosInstruccion = 1;  
        byte tipoOpA = 0, tipoOpB = 0;int valorOpA = 0, valorOpB = 0;int cantBytesOpA = 0, cantBytesOpB = 0;
        String instHexa = ""; String lineaDissasembler = "";	
        if (codOperacion >= 0 && codOperacion <= 0x0D) {  
            tipoOpA = (byte) ((primerByte >> 6) & 0x03); 

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion); 
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;
            
            
            if (testMode) {
            	instHexa = DissasemblerAux.concatenarInstHexa(primerByte, valorOpA, null);
            	lineaDissasembler = String.format("%s %s",
            			DissasemblerAux.codOpAMnemonico(codOperacion),
            			DissasemblerAux.decodificarOp(registros, tipoOpA, valorOpA));
            }
            	
        } 

        else if (codOperacion >= 0x10 && codOperacion <= 0x1E) { 
        	tipoOpB = (byte) ((primerByte & 0xC0) >> 6);
        	tipoOpA = (byte) ((primerByte & 0x30) >> 4); 
            
            valorOpB = obtenerOpEnMemoria(tipoOpB, bytesYaLeidosInstruccion);
            cantBytesOpB = cantidadBytesOperando(tipoOpB);
            bytesYaLeidosInstruccion += cantBytesOpB;
            
            
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
        
        else if (codOperacion == 0x0F) {
            if (testMode) {
            	instHexa = "0F";
            	lineaDissasembler = "STOP";
            }
            
            bytesYaLeidosInstruccion = -1; 
        } else if(codOperacion == 0x0E){
        	operaciones.RET();
        	this.setJumpEjecutado(true);
        }else {
        	
        	bytesYaLeidosInstruccion = -1; 
        }

       
    	if (bytesYaLeidosInstruccion > 0 && !this.isJumpEjecutado()) { 
    		this.registros.modificaIP(bytesYaLeidosInstruccion);      
    		this.breakPointAnterior = codOperacion == 0 && valorOpA == 0xF; 
        	if ((codOperacion >= 0 && codOperacion <= 0x0D)) {
        		ejecutarOperacionUnOperando(codOperacion, tipoOpA, valorOpA);
        	}
        	else if (codOperacion >= 0x10 && codOperacion <= 0x1E) {
        		ejecutarOperacionDosOperandos(codOperacion, tipoOpA, valorOpA, tipoOpB, valorOpB);
        	}
    	} else { 
    		this.setJumpEjecutado(false);
    	}        	
    

        
        if (testMode) {
            System.out.printf("[%04X] %-20s | %s\n", registros.getIP(), instHexa, lineaDissasembler);
        }   
        
        
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
            case 11:operaciones.PUSH(tipoOp, valorOp);break;
            case 12:operaciones.POP(tipoOp,valorOp); break;
            case 13:operaciones.CALL(tipoOp,valorOp); break;
            default: break;
        }
    }
    
    
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

	public boolean getBreakPointAnterior() {
        return breakPointAnterior;
    }
    public Operaciones getOperaciones() {
        return operaciones;
    }
	public void cargarMain(List<String> parametros) {
		byte PUSH = 11;
		byte inmediato = 2;
		int argc=0,argv=-1;
		if(!(parametros.isEmpty())) {
			argv = 0;
			argc = parametros.size();
			for(String parametro: parametros) {
				argv += parametro.length()+1;
			}
		}
		
		operaciones.PUSH(inmediato, argv);
		operaciones.PUSH(inmediato, argc);
		operaciones.PUSH(inmediato, -1);
	}

        public String codOpAMnemonicoDebug(byte opcode) {
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
            case 0x0B: return "PUSH";
            case 0x0C: return "POP";
            case 0x0D: return "CALL";
            case 0x0E: return "RET";
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

}





