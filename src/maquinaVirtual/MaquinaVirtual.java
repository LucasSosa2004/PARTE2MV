package maquinaVirtual;

import java.util.List;

public class MaquinaVirtual {

    private MemoriaBase memoria;
    private Registros registros;
    private UnidadAritmeticoLogica unidadAritmeticoLogica;
    private DissasemblerAux dissasemblerAux;

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
    
    public MaquinaVirtual(HeaderMV header, List<String> parametros,  int tamMemoria, boolean testMode) {
    	this.tablaV2 = new TablaDescripSegmentosV2();
    	this.memoria = new MemoriaV2(header,parametros, tamMemoria,tablaV2);
    	this.registros = new Registros(header,tablaV2);
        this.unidadAritmeticoLogica = new UnidadAritmeticoLogica(registros, memoria, testMode);
        this.unidadAritmeticoLogica.cargarMain(parametros);
        this.dissasemblerAux = new DissasemblerAux(registros, memoria);
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
    

	public boolean isJumpEjecutado() {
        return unidadAritmeticoLogica.isJumpEjecutado();
    }

    public void setJumpEjecutado(boolean jumpEjecutado) {
        unidadAritmeticoLogica.setJumpEjecutado(jumpEjecutado);
    }

    public boolean caidaSegmentoIP() {
        //Convertir el IP en una dir fisica
        int IP = this.registros.getIP();
        int DS = this.registros.getDS();

        int IPdirFisica = this.memoria.getDireccionFisica(IP);
        int DSdirFisica = this.memoria.getDireccionFisica(DS);

        return IPdirFisica + 1 > DSdirFisica;
    }

    
    
}