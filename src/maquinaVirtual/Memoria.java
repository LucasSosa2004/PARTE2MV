package maquinaVirtual;


public class Memoria implements MemoriaBase {

	private int kib = 16384;
	private byte[] memoria = new byte[kib];
	TablaDescripSegmentos tabla; 
	
	public Memoria(int tamanoCodigo) {
		this.tabla = new TablaDescripSegmentos((short)tamanoCodigo,kib);
	}
	
	public int getDireccionFisica(int direccionLogica) {

		short segmento = (short)(direccionLogica >> 16);
		short offset = (short)(direccionLogica);
		int direccionFisica = 0;
		
		if(segmento == 0) {
			direccionFisica = this.tabla.getCS();
		}
		else if (segmento == 1) {
			direccionFisica = this.tabla.getDS();
		}
		
		direccionFisica += offset;
		return direccionFisica;
		
	}
	
	public int getKib() {
		return this.kib;
	}
	
	public void cargarByteAMemoria(byte byteLeido, int posicion) { //posicion = byteLeidos
		if (posicion >= this.memoria.length) {
            throw new IndexOutOfBoundsException("Posicion invalida: " + posicion + " (debe estar entre 0 y " + (memoria.length - 1) + ")");
		}
		
		this.memoria[posicion] = byteLeido;
	}
	
	public void cargarByte(byte byteACargar, int direccionLogica) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	memoria[direccionFisica] = byteACargar;
    }
	
	// Metodo para leer un byte desde una direccion fisica (Se usa internamente en esta clase)
	//Las otras clases usan leerPrimerByte o leerOperando, ya que reciben dir_logicas
	 public byte leerByte(int direccionFisica) {
	     if (direccionFisica < 0 || direccionFisica >= kib) {
	         throw new IllegalArgumentException("Direccion fisica fuera de los limites de la memoria: " + direccionFisica);
	     }
	     return memoria[direccionFisica];
	 }
	 
	 @Override
	 public byte leerPrimerByte(int dirLogicaIP) {
		int dirFisicaIP = getDireccionFisica(dirLogicaIP); 
		return leerByte(dirFisicaIP);
	 }

	
	public int leerOperando(int direccionLogica, int bytesYaLeidos, int cantidadBytes) {
	    int direccionFisica = getDireccionFisica(direccionLogica);
	    direccionFisica += bytesYaLeidos;

	    int valor = 0;
	    for (int i = 0; i < cantidadBytes; i++) {
	        valor = (valor << 8) | (memoria[direccionFisica + i] & 0xFF);
	    }
	    return valor;
	}
	 
	public int leerMemoria(int direccionLogica, int cantidadBytes) {
	    int direccionFisica = getDireccionFisica(direccionLogica);
	    cantidadBytes = 4; //por defecto en v1
	    int valor = 0;
	    for (int i = 0; i < cantidadBytes; i++) {
	        valor = (valor << 8) | (memoria[direccionFisica + i] & 0xFF);
	    }
	    return valor;
	}


	
	public void escribirOperando(int direccionLogica, int valor) {
		
	    int direccionFisica = getDireccionFisica(direccionLogica);
	    
	    // Validar que se disponga de 4 bytes a partir de la direccion fisica en memoria
	    if (direccionFisica < 0 || direccionFisica + 3 >= kib) {
	        throw new IllegalArgumentException("Direccion fuera de los limites de la memoria: " + direccionFisica);
	    }
	    
	    // Escribir el valor en memoria
	    memoria[direccionFisica]     = (byte) ((valor >> 24) & 0xFF);
	    memoria[direccionFisica + 1] = (byte) ((valor >> 16) & 0xFF);
	    memoria[direccionFisica + 2] = (byte) ((valor >> 8) & 0xFF);
	    memoria[direccionFisica + 3] = (byte) (valor & 0xFF);
	}
	
    public void escribirOperando(int direccionLogica, int valor,int cantBytes) {
        int direccionFisica = getDireccionFisica(direccionLogica);
	    cantBytes = 4;
        if (direccionFisica < 0 || direccionFisica + cantBytes >= kib) {
	        throw new IllegalArgumentException("Direccion fuera de los limites de la memoria: " + direccionFisica);
	    }
	    
        for (int i = 0; i < cantBytes; i++) {
            memoria[direccionFisica + i] = (byte) ((valor >> ((4 - 1 - i) * 8)) & 0xFF);
        }
    }
	
	public int agregarOffset(int direccionLogica, int offsetAdicional) {
	    int offset = (direccionLogica & 0xFFFF) + offsetAdicional; // Extraer solo el offset (los 16 bits bajos)
	    offset &= 0xFFFF; // Asegurarse de que no se pase de 16 bits
	    int segmento = direccionLogica & 0xFFFF0000; // Conservar el segmento (los 16 bits altos)
	    return segmento | offset;
	}
    public void escribirByteLogica(int direccionLogica, byte valor) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	escribirByte(direccionFisica,valor);
    }
	public byte leerByteLogica(int direccionLogica) {
        return leerByte(getDireccionFisica(direccionLogica));
    }
	public int leerPila(int direccionLogica) {throw new RuntimeException("Version 1 no tiene pila");}
	public void imprimirMemoria(int direccionLogica, int offset) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	for(int i=direccionFisica; i<direccionFisica + offset;i++)
    		System.out.println(i + ": "+ Integer.toHexString(memoria[i]& 0xFF));
    }
	
    public byte[] getMemoriaRaw() {
        return memoria;
    }
    public void escribirByte(int direccionFisica,byte valor) {
    	memoria[direccionFisica] = valor;
    }

    public int getTamano() {
    	return kib;
    }
    public void escribirPila(int direccionLogica,int valor) {
    	throw new IllegalArgumentException("v1 no tiene pila");
    }
	
}