package maquinaVirtual;

import java.util.HashMap;


public class Registros {

	private HashMap<String, Integer> registros = new HashMap<String, Integer>();
	private MemoriaBase memoria;

	public Registros(TablaDescripSegmentosV2 tabla) {
		this.registros = new HashMap<String,Integer>();
	}
	
	public Registros(MemoriaBase memoria) {
		this.registros.put("CS",0x0);
		this.registros.put("DS",0x00010000); //pos de DS en TablaDS
		this.registros.put("IP", 0x0);
		this.registros.put("CC", 0x0);
		this.registros.put("AC", 0x0);
		this.registros.put("EAX", 0x0);
		this.registros.put("EBX", 0x0);
		this.registros.put("ECX", 0x0);
		this.registros.put("EDX", 0x0);
		this.registros.put("EEX", 0x0);
		this.registros.put("EFX", 0x0);
		
		this.memoria = memoria;
	}
	
	public void cargarRegistrosV2(TablaDescripSegmentosV2 tabla) {
		this.registros.put("CS", tabla.inincializarRegistro("CS"));
		this.registros.put("DS", tabla.inincializarRegistro("DS"));
		this.registros.put("ES", tabla.inincializarRegistro("ES"));
		this.registros.put("SS", tabla.inincializarRegistro("SS"));
		this.registros.put("KS", tabla.inincializarRegistro("KS"));
		this.registros.put("IP", tabla.inincializarRegistro("CS") + tabla.getEntryPoint());
		this.registros.put("SP", tabla.inicializarSP());
		this.registros.put("BP", 0x0);
		this.registros.put("CC", 0x0);
		this.registros.put("AC", 0x0);
		this.registros.put("EAX", 0x0);
		this.registros.put("EBX", 0x0);
		this.registros.put("ECX", 0x0);
		this.registros.put("EDX", 0x0);
		this.registros.put("EEX", 0x0);
		this.registros.put("EFX", 0x0);
	}
	
	
	
    public int getRegistro(String nombre) {
        return registros.getOrDefault(nombre, 0);
    }
    public int getRegistro(int index) {
    	String nom = getNombreRegistro(index);
    	return registros.getOrDefault(nom,0);
    }
    
    public void setRegistro(String nombre, int valor) {
        registros.put(nombre, valor);
    }
    public void addRegistro(int cod, int val) {
    	String reg = getNombreRegistro(cod);
    	registros.put(reg, val);
    }
    
    public String getNombreRegistro(int codigoRegistro) {
        switch (codigoRegistro) {
            case 0:  return "CS";
            case 1:  return "DS";
            case 2:  return "ES";
            case 3:  return "SS";
            case 4:  return "KS";
            case 5:  return "IP";
            case 6:  return "SP";
            case 7:  return "BP";
            case 8:  return "CC";
            case 9:  return "AC";
            case 10: return "EAX";
            case 11: return "EBX";
            case 12: return "ECX";
            case 13: return "EDX";
            case 14: return "EEX";
            case 15: return "EFX";
            default:
                throw new IllegalArgumentException("Codigo de registro invalido: " + codigoRegistro);
        }
    }
    
    public void setRegistro(int cod, int val) {
    	String reg = getNombreRegistro(cod);
    	registros.replace(reg, val);
    }
    
    public void modificaIP(int cantBytes) { //Se recibe la cant bytes que se debe avanzar (Se actualiza solo el offset)
        int ip = getIP();
        int segmento = ip & 0xFFFF0000;  // Extrae los 16 bits superiores.
        int offset   = ip & 0x0000FFFF;    // Extrae los 16 bits inferiores.
        offset = (offset + cantBytes) & 0xFFFF; // Se suma y se restringe a 16 bits.
        int nuevoIP = segmento | offset;
        setIP(nuevoIP);
    }
    
    public int getDirFisicaIP() {
        int ipLogico = getIP(); // Obtenemos el puntero logico almacenado en IP
        return memoria.getDireccionFisica(ipLogico);
    }
    

    public int leerSectorRegistro(int operando) {

    	int codRegistro = (operando >>> 4) & 0x0F;
    	int sectorRegistro = (operando >>> 2) & 0x03;
    	
    	String nombreRegistro = getNombreRegistro(codRegistro);
        int valorCompleto = getRegistro(nombreRegistro); //getRegistro(nombre) devuelve un int (32 bits) con el contenido completo del registro

        switch (sectorRegistro) {
            case 0b00:return valorCompleto; // Entero completo (32 bits)
            case 0b01:return valorCompleto & 0xFF; // Byte mas bajo (ej: AL)  bits 0..7
            case 0b10:return (valorCompleto >>> 8) & 0xFF; // Segundo byte mas bajo (ej: AH)  bits 8..15            
            case 0b11:return valorCompleto & 0xFFFF; // Dos bytes bajos (ej: AX)  bits 0..15
            default:
                throw new IllegalArgumentException("Sector invalido: " + sectorRegistro);
        }
    }
    
    public int escribirSectorRegistro(int operando, int nuevoValor) {
    	
        int codRegistro = (operando >>> 4) & 0x0F;
        int sectorRegistro = (operando >>> 2) & 0x03;
        String nombreRegistro = getNombreRegistro(codRegistro);
        int valorCompleto = getRegistro(nombreRegistro); // Obtener el valor completo (32 bits) del registro.
        int cantBytesOp;
        // Modificar la parte del registro especificada por el sector
        switch (sectorRegistro) {
            case 0b00:valorCompleto = nuevoValor;cantBytesOp=4;break; // Reemplaza todos los 32 bits del registro.
            case 0b01:valorCompleto = (valorCompleto & 0xFFFFFF00) | (nuevoValor & 0xFF);cantBytesOp=1;break; // Reemplaza solo el byte mas bajo (bits 0..7)
            case 0b10:valorCompleto = (valorCompleto & 0xFFFF00FF) | ((nuevoValor & 0xFF) << 8);cantBytesOp=1;break; // Reemplaza el segundo byte mas bajo (bits 8..15)
            case 0b11:valorCompleto = (valorCompleto & 0xFFFF0000) | (nuevoValor & 0xFFFF);cantBytesOp=2;break;// Reemplaza los dos bytes bajos (bits 0..15)
            default:
                throw new IllegalArgumentException("Sector invalido: " + sectorRegistro);
        }
        
        System.out.println("Registro escrito"+ nombreRegistro+ "valor"+ valorCompleto);
        // Actualiza el registro con el nuevo valor.
        setRegistro(nombreRegistro, valorCompleto);
        //System.out.println("valor actualizado del registro CX "+ this.getECX());
        //System.out.println("Valor registro CC "+ Integer.toBinaryString(getCC()));
        return cantBytesOp;

    }
    
    public void modificarCC(int ultimoResultado, int numBytes) {
        // 1) Truncado al ancho real: numBytes * 8 bits
    	int SF = 1 << 31;
    	int ZF = 1 << 30;  // Zero Flag (bit 30)
        int bits = numBytes * 8;
        int mask = (bits == 32) ? 0xFFFF_FFFF : ((1 << bits) - 1);
        int resTrunc = ultimoResultado & mask;

        // 2) Limpiar solo SF y ZF
        int cc = getCC() & ~(SF | ZF);

        // 3) Sign Flag: si el MSB dentro de resTrunc esta activo
        int signMask = 1 << (bits - 1);
        if ((resTrunc & signMask) != 0) {
            cc |= SF;
        }

        // 4) Zero Flag: si resTrunc == 0
        if (resTrunc == 0) {
            cc |= ZF;
        }

        // 5) Guardar CC actualizado
        setCC(cc);
    }
    
    
    //Getters y setters
    
    
    
    public int getCS() {
        return getRegistro("CS");
    }

    public void setCS(int nuevoCS) {
        setRegistro("CS", nuevoCS);
    }

    
    public int getDS() {
        return getRegistro("DS");
    }

    public void setDS(int nuevoDS) {
        setRegistro("DS", nuevoDS);
    }

    public int getES() {
        return getRegistro("ES");
    }

    public void setES(int nuevoES) {
        setRegistro("ES", nuevoES);
    }

    public int getSS() {
        return getRegistro("SS");
    }

    public void setSS(int nuevoSS) {
        setRegistro("SS", nuevoSS);
    }

    public int getKS() {
        return getRegistro("KS");
    }

    public void setKS(int nuevoKS) {
        setRegistro("KS", nuevoKS);
    }


    public int getIP() {
        return getRegistro("IP");
    }

    public void setIP(int nuevoIP) {
        setRegistro("IP", nuevoIP);
    }

    public int getSP() {
        return getRegistro("SP");
    }

    public void setSP(int nuevoSP) {
        setRegistro("SP", nuevoSP);
    }

    public int getBP() {
        return getRegistro("BP");
    }

    public void setBP(int nuevoBP) {
        setRegistro("BP", nuevoBP);
    }

    public int getCC() {
        return getRegistro("CC");
    }

    public void setCC(int nuevoCC) {
        setRegistro("CC", nuevoCC);
    }

    public int getAC() {
        return getRegistro("AC");
    }

    public void setAC(int nuevoAC) {
        setRegistro("AC", nuevoAC);
    }

    public int getEAX() {
        return getRegistro("EAX");
    }

    public void setEAX(int nuevoEAX) {
        setRegistro("EAX", nuevoEAX);
    }

    public int getEBX() {
        return getRegistro("EBX");
    }

    public void setEBX(int nuevoEBX) {
        setRegistro("EBX", nuevoEBX);
    }

    public int getECX() {
        return getRegistro("ECX");
    }

    public void setECX(int nuevoECX) {
        setRegistro("ECX", nuevoECX);
    }

    public int getEDX() {
        return getRegistro("EDX");
    }

    public void setEDX(int nuevoEDX) {
        setRegistro("EDX", nuevoEDX);
    }

    public int getEEX() {
        return getRegistro("EEX");
    }

    public void setEEX(int nuevoEEX) {
        setRegistro("EEX", nuevoEEX);
    }

    public int getEFX() {
        return getRegistro("EFX");
    }

    public void setEFX(int nuevoEFX) {
        setRegistro("EFX", nuevoEFX);
    }
    
    public void mostrarRegistros() {
		 for (HashMap.Entry<String, Integer> entrada : registros.entrySet()) {
	         System.out.println(entrada.getKey() + ": " +  Integer.toHexString(entrada.getValue() & 0xFFFFFFFF));
	     }
    }
    public static String formatoBinario(int valor) {
        String binario = String.format("%32s", Integer.toBinaryString(valor)).replace(' ', '0');
        return binario.replaceAll("(.{8})(?=.)", "$1 ");
    }
    
	
	
		
} 
