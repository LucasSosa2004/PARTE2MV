package maquinaVirtual;

public interface MemoriaBase {
	 int getDireccionFisica(int direccionLogica);
	    void cargarByteAMemoria(byte byteLeido, int posicion);
	    byte leerByte(int direccionFisica);
	    byte leerByteLogica(int direccionLogica);
	    int leerOperando(int direccionLogica, int bytesYaLeidos, int cantidadBytes);
	    int leerMemoria(int direccionLogica, int cantidadBytes);
	    void escribirOperando(int direccionLogica, int valor);
	    int agregarOffset(int direccionLogica, int offsetAdicional) ;
	       
	    byte leerPrimerByte(int dirLogicaIP);
	    public void imprimirMemoria(int direccionLogica, int offset);
	    public void cargarByte(byte byteACargar, int direccionLogica);
	    public int leerPila(int direccionLogica);
	    public byte[] getMemoriaRaw();
	    public void escribirByte(int direccionLogica,byte valor);
	    public int getTamano();
	    public void escribirPila(int direccionLogica,int valor);
	    public void escribirByteLogica(int direccionLogica, byte valor);
	    public void escribirOperando(int direccionLogica, int valor,int cantBytes);
	    public void escribirPila(int direccionLogica, int valor, int cantBytes);
}
