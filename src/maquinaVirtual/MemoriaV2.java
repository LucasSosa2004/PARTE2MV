package maquinaVirtual;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MemoriaV2 implements MemoriaBase {
    private final byte[] memoria;
    private final Map<String, Integer> baseSegmentos = new HashMap<>();
    private final Map<String, Integer> tamanios = new HashMap<>();
    private final TablaDescripSegmentosV2 tabla;
    private final int tamanoMemoria;

    public MemoriaV2(List<String> parametros, int tamanoMemoria, TablaDescripSegmentosV2 tabla) {
        this.memoria = new byte[tamanoMemoria];
        this.tabla = tabla;
        this.tamanoMemoria = tamanoMemoria;
        chequearMemoria(tamanoMemoria,tabla);
        int offset = 0;
        if (!parametros.isEmpty()){
        	cargarParametros(parametros,0); // agrega el ParamSegment a la Tabla
        }
    }
     
    private void chequearMemoria(int tamanoMemoria, TablaDescripSegmentosV2 tabla) {
    	int tot=0;
    	for(DescriptorSegmento seg : tabla.getSegmentos()) {
    		tot += seg.getTamanio();    		
    	}
    	if(tot>tamanoMemoria)
    		throw new IllegalStateException("Memoria insuficiente");
    }
    
    private void cargarParametros(List<String> parametros, int offset) {
        int inicioStrings = 0;
        List<Integer> offsets = new ArrayList<>();

        // 1. Escribir strings con terminador '\0'
        int posicionActual = inicioStrings;
        for (String p : parametros) {
            byte[] bytes = p.getBytes();
            System.arraycopy(bytes, 0, memoria, posicionActual, bytes.length);
            posicionActual += bytes.length;
            memoria[posicionActual++] = 0x00;
            offsets.add(posicionActual - bytes.length - 1);
        }

        // 2. Escribir punteros (cada puntero = 4 bytes, 0x0000 + offset)
        for (int offsetStr : offsets) {
            memoria[posicionActual++] = 0x00;
            memoria[posicionActual++] = 0x00;
            memoria[posicionActual++] = (byte) ((offsetStr >> 8) & 0xFF);
            memoria[posicionActual++] = (byte) (offsetStr & 0xFF);
        }

        tabla.agregarSegmento("PS", (short)0, (short)(posicionActual - 1));
        System.out.println(getTabla().getSegmento(0).toString());
    }

    public void cargarSegmentoDesdeArchivo(String segmento, FileInputStream fis, int longitud) throws IOException {
        int base = baseSegmentos.get(segmento);
        for (int i = 0; i < longitud; i++) {
            int b = fis.read();
            if (b == -1) throw new IOException("Archivo incompleto al leer segmento " + segmento);
            memoria[base + i] = (byte) b;
        }
    }
    
    public void printPosiciones(int inicio, int fin) {
    	for(int i= inicio; i<fin; i++) {
    		System.out.println(memoria[i]);
    	}
    }

    public int getDireccionBaseSegmento(String nombre) {
        return baseSegmentos.getOrDefault(nombre, 0);
    }

    public byte[] getMemoriaRaw() {
        return memoria;
    }

    public TablaDescripSegmentosV2 getTabla() {
    	return tabla;
    }
 
    public int leerPila(int direccionLogica) {
        int direccionFisica = getDireccionFisica(direccionLogica);


        if (direccionFisica + 3 > tabla.getSegmento("SS").getLimite()) {
            throw new IndexOutOfBoundsException("Stack Underflow");
        }

        int valor = 0;
        valor  = (memoria[direccionFisica--] & 0xFF);
        valor |= (memoria[direccionFisica--] & 0xFF) << 8;
        valor |= (memoria[direccionFisica--] & 0xFF) << 16;
        valor |= (memoria[direccionFisica--] & 0xFF) << 24;

        return valor;
    }


    public void escribirByte(int direccionFisica,byte valor) {
    	memoria[direccionFisica] = valor;
    }

    @Override
	 public byte leerPrimerByte(int dirLogicaIP) {
		int dirFisicaIP = getDireccionFisica(dirLogicaIP); 
		return leerByte(dirFisicaIP);
	 }

    @Override
    public int getDireccionFisica(int direccionLogica) {
        short segmento = (short) (direccionLogica >> 16);
        short offset = (short) direccionLogica;
        int base = tabla.getBase(segmento);
        return base + offset;
    }
    
    @Override
    public int agregarOffset(int direccionLogica, int offsetAdicional) {
	    int offset = (direccionLogica & 0xFFFF) + offsetAdicional; // Extraer solo el offset (los 16 bits bajos)
	    offset &= 0xFFFF; // Asegurarse de que no se pase de 16 bits
	    int segmento = direccionLogica & 0xFFFF0000; // Conservar el segmento (los 16 bits altos)
	    return segmento | offset;
	}

    @Override
    public void cargarByteAMemoria(byte byteLeido, int posicion) {
        memoria[posicion] = byteLeido;
    }
    
    public void cargarByte(byte byteACargar, int direccionLogica) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	memoria[direccionFisica] = byteACargar;
    }

    
    @Override
    public byte leerByte(int direccionFisica) {
        return memoria[direccionFisica];
    }

    @Override
    public byte leerByteLogica(int direccionLogica) {
        return leerByte(getDireccionFisica(direccionLogica));
    }

    @Override
    public int leerOperando(int direccionLogica, int bytesYaLeidos, int cantidadBytes) {
        int direccionFisica = getDireccionFisica(direccionLogica) + bytesYaLeidos;
        int valor = 0;
        for (int i = 0; i < cantidadBytes; i++) {
            valor = (valor << 8) | (memoria[direccionFisica + i] & 0xFF);
        }
        return valor;
    }

    @Override
    public int leerMemoria(int direccionLogica, int cantidadBytes) {
        int direccionFisica = getDireccionFisica(direccionLogica);
        int valor = 0;
        for (int i = 0; i < cantidadBytes; i++) {
            valor = (valor << 8) | (memoria[direccionFisica + i] & 0xFF);
        }
        return valor;
    }

    @Override
    public void escribirOperando(int direccionLogica, int valor) {
        int direccionFisica = getDireccionFisica(direccionLogica);
        int tamanio = valor & 0b11;
        tamanio = 4 - tamanio; 
        for(int i=0; i<tamanio;i++) {
        	memoria[direccionFisica + tamanio-i-1] = (byte) ((valor >> i*8) & 0xFF);
        }
        /*
        memoria[direccionFisica]     = (byte) ((valor >> 24) & 0xFF);
        memoria[direccionFisica + 1] = (byte) ((valor >> 16) & 0xFF);
        memoria[direccionFisica + 2] = (byte) ((valor >> 8) & 0xFF);
        memoria[direccionFisica + 3] = (byte) (valor & 0xFF);*/
    }
    
    public void imprimirMemoria(int direccionLogica, int offset) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	for(int i=direccionFisica; i<direccionFisica + offset;i++)
    		System.out.println(tabla.getSegmentoDirFisica(i) + ": " +i + ": "+ Integer.toHexString(memoria[i] & 0xFF));
    }
    public int getTamano() {
    	return this.tamanoMemoria;
    }
}
