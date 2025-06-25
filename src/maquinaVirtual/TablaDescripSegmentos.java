package maquinaVirtual;


public class TablaDescripSegmentos {
    private short[][] tablaDS = new short[8][2]; 

    public TablaDescripSegmentos(short CS, int tamanio) {
        this.tablaDS[0][0] = 0;
        this.tablaDS[0][1] = CS;
        this.tablaDS[1][0] = CS;
        this.tablaDS[1][1] = (short)tamanio; 
    }

    public short getCS() {
        return this.tablaDS[0][0];

    }

    public short getTamanioCodigo() { 
        return this.tablaDS[0][1]; 
    }

    public short getDS() {
        return this.tablaDS[1][0];
    }

    public short getTamanio() {
        return this.tablaDS[1][1];
    }
}