package maquinaVirtual;

import java.util.List;

public class MaquinaVirtual {
    private MemoriaBase memoria;
    private Registros registros;
    private UnidadAritmeticoLogica unidadAritmeticoLogica;
    private DissasemblerAux dissasemblerAux;
    private TablaDescripSegmentosV2 tablaV2;
    private DissasemblerV2 disassemblerV2;
    private boolean esVersion2;

    public MaquinaVirtual(int tamanoCodigo, boolean testMode) {
        this.memoria = new Memoria(tamanoCodigo);
        this.registros = new Registros(memoria);
        this.unidadAritmeticoLogica = new UnidadAritmeticoLogica(registros, memoria, testMode);
        this.dissasemblerAux = new DissasemblerAux(registros, memoria);
        this.esVersion2 = false;
    }
    
    public MaquinaVirtual(HeaderMV header, List<String> parametros, int tamMemoria, Archivos archivos, boolean testMode) {
        this.tablaV2 = new TablaDescripSegmentosV2(header);
        this.memoria = new MemoriaV2(parametros, tamMemoria, tablaV2);
        this.tablaV2.setMemoria(this.memoria);
        this.tablaV2.setTabla(header);
        this.registros = new Registros(tablaV2);
        
        // Solo cargamos los registros si no estamos cargando desde un VMI
        if (!archivos.tieneVMI()) {
            this.registros.cargarRegistrosV2(tablaV2);
            // Solo cargamos los parámetros si no estamos cargando desde un VMI
            this.unidadAritmeticoLogica = new UnidadAritmeticoLogica(registros, memoria, tablaV2, archivos, testMode);
            this.unidadAritmeticoLogica.cargarMain(parametros);
        } else {
            this.unidadAritmeticoLogica = new UnidadAritmeticoLogica(registros, memoria, tablaV2, archivos, testMode);
        }
        
        this.dissasemblerAux = new DissasemblerAux(registros, memoria);
        this.disassemblerV2 = new DissasemblerV2(registros, memoria, tablaV2);
        this.esVersion2 = true;
    }
    
    public void ejecutar() {
        while (true) {
            byte primerByte = memoria.leerByte(registros.getDirFisicaIP());
            int bytesLeidos;
            
            if (esVersion2) {
                bytesLeidos = disassemblerV2.decodificarInstruccion(primerByte);
            } else {
                bytesLeidos = dissasemblerAux.decodificarInstruccion(primerByte);
            }
            
            if (bytesLeidos == -1) {
                System.out.println("Error: Instrucción inválida");
                break;
            }
            
            registros.modificaIP(bytesLeidos);
            
            if (primerByte == 0x0F) { // STOP
                break;
            }
        }
    }
    
    public void mostrarCadenas() {
        if (esVersion2) {
            disassemblerV2.mostrarCadenas();
        }
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
    
    public DissasemblerV2 getDissasemblerV2() {
        return this.disassemblerV2;
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
        int IP = this.registros.getIP();
        int DS = this.registros.getDS();
        int IPdirFisica = this.memoria.getDireccionFisica(IP);
        int DSdirFisica = this.memoria.getDireccionFisica(DS);
        return IPdirFisica + 1 > DSdirFisica;
    }
}