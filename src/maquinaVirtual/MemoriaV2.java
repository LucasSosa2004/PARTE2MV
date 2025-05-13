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

    public MemoriaV2(HeaderMV header, List<String> parametros, int tamanoMemoria, TablaDescripSegmentosV2 tabla) {
        this.memoria = new byte[tamanoMemoria];
        this.tabla = tabla;
        int offset = 0;
        if (!parametros.isEmpty()){
        	cargarParametros(parametros,0); // agrega el ParamSegment a la Tabla
        }
        
        tabla.setTabla(header);
        
        
        /*lo de abajo se haria en tablaDescrSegv2 (menos cargar el param seg)
         * 
        // Param Segment (si hay parametros)
        if (!parametros.isEmpty()) {
            int tamanoP = cargarParametros(parametros, offset);
            baseSegmentos.put("P", offset);
            tamanios.put("P", tamanoP);
            tabla.agregarSegmento(offset, tamanoP);
            offset += tamanoP;
        }

        // Const Segment (KS)
        if (header.getTamanoKS() > 0) {
            baseSegmentos.put("KS", offset);
            tamanios.put("KS", header.getTamanoKS());
            tabla.agregarSegmento(offset, header.getTamanoKS());
            offset += header.getTamanoKS();
        }

        // Code Segment (CS)
        if (header.getTamanoCS() > 0) {
            baseSegmentos.put("CS", offset);
            tamanios.put("CS", header.getTamanoCS());
            tabla.agregarSegmento(offset, header.getTamanoCS());
            offset += header.getTamanoCS();
        }

        // Data Segment (DS)
        if (header.getTamanoDS() > 0) {
            baseSegmentos.put("DS", offset);
            tamanios.put("DS", header.getTamanoDS());
            tabla.agregarSegmento(offset, header.getTamanoDS());
            offset += header.getTamanoDS();
        }

        // Extra Segment (ES)
        if (header.getTamanoES() > 0) {
            baseSegmentos.put("ES", offset);
            tamanios.put("ES", header.getTamanoES());
            tabla.agregarSegmento(offset, header.getTamanoES());
            offset += header.getTamanoES();
        }

        // Stack Segment (SS)
        if (header.getTamanoSS() > 0) {
            baseSegmentos.put("SS", offset);
            tamanios.put("SS", header.getTamanoSS());
            tabla.agregarSegmento(offset, header.getTamanoSS());
            offset += header.getTamanoSS();
        }*/
    }

    /**
     * Carga los par�metros del programa en el segmento de par�metros.
     * Este segmento se ubicar� siempre desde la posici�n 0x00000000.
     *
     * @param parametros Lista de strings le�dos desde la consola
     * @param offset     Posici�n inicial en memoria (debe ser 0)
     * @return El tama�o total del segmento de par�metros en bytes
     */
    
     //esto lo hizo juani de otra manera 
    /*
     public void cargarParametros(List<String> parametros) {
        List<Integer> punteros;
        int posActual=0;
        for(int i=0; i<parametros.size();i++) {
            char car = parametros.get(i).charAt(posActual); 
            while(posActual<car){
                memoria.cargarByteAMemoria((byte) car, posActual); // offset j
                posActual++;
            }
            memoria.cargarByteAMemoria(0, posActual); // \0
            punteros.add(posActual);
        }
        short offset = 0;
        for(int i : punteros) {
            memoria.escribirOperando(posActual + offset, i);
            offset += 4;
        }
        memoria.tabla.agregarSegmento("PS", 0, posActual + offset);
    }*/
     
    private int cargarParametros(List<String> parametros, int offset) {
        int inicioStrings = offset;
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

        tabla.agregarSegmento("PS", (short)0, (short)(posicionActual + offset));
        System.out.println(getTabla().getSegmento(0).toString());
        return posicionActual - offset;
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
        memoria[direccionFisica]     = (byte) ((valor >> 24) & 0xFF);
        memoria[direccionFisica + 1] = (byte) ((valor >> 16) & 0xFF);
        memoria[direccionFisica + 2] = (byte) ((valor >> 8) & 0xFF);
        memoria[direccionFisica + 3] = (byte) (valor & 0xFF);
    }
    
    public void imprimirMemoria(int direccionLogica, int offset) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	for(int i=direccionFisica; i<direccionFisica + offset;i++)
    		System.out.println(i + ": "+ Integer.toHexString(memoria[i] & 0xFF));
    }
}
