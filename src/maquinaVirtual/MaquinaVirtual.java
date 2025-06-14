package maquinaVirtual;

import java.util.List;

public class MaquinaVirtual {

    private MemoriaBase memoria;
    private Registros registros;
    private UnidadAritmeticoLogica unidadAritmeticoLogica;
    private DissasemblerAux dissasemblerAux;
    private DisassemblerV2 disassemblerV2;

    private MemoriaV2 memoriaV2;
    private TablaDescripSegmentosV2 tablaV2;

    public MaquinaVirtual(int tamanoCodigo, boolean testMode) {

        this.memoria = new Memoria(tamanoCodigo);
        this.registros = new Registros(memoria);
        this.unidadAritmeticoLogica = new UnidadAritmeticoLogica(registros, memoria, testMode);
        this.dissasemblerAux = new DissasemblerAux(registros, memoria);
    }
    
    //v2 
    //MaquinaVirtual(header, parametrosPrograma, tamMemoria);
    public MaquinaVirtual(List<String> parametros,  int tamMemoria, Archivos archivos, boolean testMode) {
    	this.tablaV2 = new TablaDescripSegmentosV2();
    	this.memoria = new MemoriaV2(parametros, tamMemoria,tablaV2);
    	this.registros = new Registros(tablaV2);
    	this.unidadAritmeticoLogica = new UnidadAritmeticoLogica(registros, memoria, tablaV2, archivos, testMode);
    	this.dissasemblerAux = new DissasemblerAux(registros,memoria);
    	this.disassemblerV2 = new DisassemblerV2(registros, memoria, tablaV2);
    }
    
    public MaquinaVirtual(HeaderMV header, List<String> parametros,  int tamMemoria, Archivos archivos, boolean testMode) {
    	this.tablaV2 = new TablaDescripSegmentosV2(header);
    	this.memoria = new MemoriaV2(parametros, tamMemoria,tablaV2);
    	this.tablaV2.setTabla(header);
    	this.registros = new Registros(tablaV2);
    	this.registros.cargarRegistrosV2(tablaV2);
        this.unidadAritmeticoLogica = new UnidadAritmeticoLogica(registros, memoria, tablaV2, archivos, testMode);
        this.unidadAritmeticoLogica.cargarMain(parametros);
        this.dissasemblerAux = new DissasemblerAux(registros,memoria);
        this.disassemblerV2 = new DisassemblerV2(registros, memoria, tablaV2);
    }

    
    

    public void setCSOperaciones(int CS) {
        this.unidadAritmeticoLogica.getOperaciones().setCS(CS);
    }
    
    public MemoriaBase getMemoria() {
        return this.memoria;
    }
    public Registros getRegistros() {
        return this.registros;
    }
    public UnidadAritmeticoLogica getUnidadAritmeticoLogica() {
        return this.unidadAritmeticoLogica;
    }

    public DissasemblerAux getDissasemblerAux() {
        return this.dissasemblerAux;
    }
    
    public TablaDescripSegmentosV2 getTabla() {
    	return this.tablaV2;
    }
    
    public void mostrarCadenas() {
        disassemblerV2.mostrarCadenas();        
    }

    public DisassemblerV2 getDissasemblerV2() {
        return this.disassemblerV2;
    }
	public boolean isJumpEjecutado() {
        return unidadAritmeticoLogica.isJumpEjecutado();
    }

    public void setJumpEjecutado(boolean jumpEjecutado) {
        unidadAritmeticoLogica.setJumpEjecutado(jumpEjecutado);
    }

    /*
    //si el siguient a CS no es DS 
    public boolean caidaSegmentoIP() {
        //Convertir el IP en una dir fisica
        int IP = this.registros.getIP();
        int DS = this.registros.getDS();

        int IPdirFisica = this.memoria.getDireccionFisica(IP);
        int DSdirFisica = this.memoria.getDireccionFisica(DS); //siguiente aCS

        return IPdirFisica + 1 > DSdirFisica;
    }*/

    public boolean caidaSegmentoIP() {
        //Convertir el IP en una dir fisica
        int IP = this.registros.getIP();
        //registros.mostrarRegistros();
        int IPdirFisica = this.memoria.getDireccionFisica(IP);
        if(tablaV2 == null) {
            int DS = this.registros.getDS();
            int DSdirFisica = this.memoria.getDireccionFisica(DS); 
            return IPdirFisica + 1 > DSdirFisica;
        }
        else {
        	DescriptorSegmento CS = tablaV2.getSegmento("CS");
        	int finCS = CS.getBase()+CS.getTamanio();
        	//System.out.println(Integer.toHexString(IPdirFisica) +" "+ finCS);
        	return IPdirFisica + 1 > finCS;
        }
        
    }
    

    
    
}