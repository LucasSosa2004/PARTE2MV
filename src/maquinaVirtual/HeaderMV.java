package maquinaVirtual;

import java.util.HashMap;

public class HeaderMV {
    private String identificador;
    private int version;

    // Para .vmx version 1
    private int tamanoCodigov1;

    // Para .vmx version 2
    private int tamanoCS;
    private int tamanoDS;
    private int tamanoES;
    private int tamanoSS;
    private int tamanoKS;
    private int entryPoint;

    private HashMap<String,Integer> segmentos = new HashMap<String, Integer>();
    
    
    // Para .vmi
    private int memoriaKiB;

    public HeaderMV(String identificador, int version) {
        this.identificador = identificador;
        this.version = version;
    }

    // Getters
    public HashMap<String,Integer> getSegmentos(){
    	return segmentos;
    }
    
    public String getIdentificador() {
        return identificador;
    }

    public int getVersion() {
        return version;
    }

    public int getTamanoCodigov1() {
        return tamanoCodigov1;
    }

    public int getTamanoCS() {
        return tamanoCS;
    }

    public int getTamanoDS() {
        return tamanoDS;
    }

    public int getTamanoES() {
        return tamanoES;
    }

    public int getTamanoSS() {
        return tamanoSS;
    }

    public int getTamanoKS() {
        return tamanoKS;
    }

    public int getEntryPoint() {
        return entryPoint;
    }

    public int getMemoriaKiB() {
        return memoriaKiB;
    }

    // Setters
    public void agregarSegmento(String segmento, int tamanio) {
    	segmentos.put(segmento, tamanio);
    }
    
    public void setTamanoCodigov1(int tamanoCodigov1) {
        this.tamanoCodigov1 = tamanoCodigov1;
    }

    public void setTamanoCS(int tamanoCS) {
        this.tamanoCS = tamanoCS;
    }

    public void setTamanoDS(int tamanoDS) {
        this.tamanoDS = tamanoDS;
    }

    public void setTamanoES(int tamanoES) {
        this.tamanoES = tamanoES;
    }

    public void setTamanoSS(int tamanoSS) {
        this.tamanoSS = tamanoSS;
    }

    public void setTamanoKS(int tamanoKS) {
        this.tamanoKS = tamanoKS;
    }

    public void setEntryPoint(int entryPoint) {
        this.entryPoint = entryPoint;
    }

    public void setMemoriaKiB(int memoriaKiB) {
        this.memoriaKiB = memoriaKiB;
    }

    // Identificadores validos
    public boolean isVmx() {
        return identificador.equals("VMX25");
    }

    public boolean isVmi() {
        return identificador.equals("VMI25");
    }

    public boolean isValido() {
        return isVmx() || isVmi();
    }

    // Representacion legible
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Identificador: ").append(identificador).append("\n");
        sb.append("Version: ").append(version).append("\n");

        if (isVmx()) {
            if (version == 1) {
                sb.append("Tamano codigo (v1): ").append(tamanoCodigov1).append("\n");
            } else if (version == 2) {
                sb.append("Tamano CS: ").append(tamanoCS).append("\n");
                sb.append("Tamano DS: ").append(tamanoDS).append("\n");
                sb.append("Tamano ES: ").append(tamanoES).append("\n");
                sb.append("Tamano SS: ").append(tamanoSS).append("\n");
                sb.append("Tamano KS: ").append(tamanoKS).append("\n");
                sb.append("Entry point: ").append(entryPoint).append("\n");
            }
        } else if (isVmi()) {
            sb.append("Tamano memoria (KiB): ").append(memoriaKiB).append("\n");
        }

        return sb.toString();
    }
}

