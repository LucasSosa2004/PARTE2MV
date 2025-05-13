package maquinaVirtual;

public interface MemoriaBase {
	 int getDireccionFisica(int direccionLogica);
	    void cargarByteAMemoria(byte byteLeido, int posicion);
	    byte leerByte(int direccionFisica);
	    byte leerByteLogica(int direccionLogica);
	    int leerOperando(int direccionLogica, int bytesYaLeidos, int cantidadBytes);
	    int leerMemoria(int direccionLogica, int cantidadBytes);
	    void escribirOperando(int direccionLogica, int valor);
	    static int agregarOffset(int direccionLogica, int offsetAdicional) {
	        int offset = (direccionLogica & 0xFFFF) + offsetAdicional;
	        offset &= 0xFFFF;
	        int segmento = direccionLogica & 0xFFFF0000;
	        return segmento | offset;
	    }
}
