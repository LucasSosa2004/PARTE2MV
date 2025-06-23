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
        System.out.println("parametro a cargar: "+parametros.get(0));
        System.out.println("se carga su valor ASCII vinculado. Ej, si es un 8 se carga el 56 ASCII");
        // 1. Escribir strings con terminador '\0'
        int posicionActual = inicioStrings;
        for (String p : parametros) {
            // Guardar el offset del inicio del string ANTES de escribirlo
            offsets.add(posicionActual);
            
            byte[] bytes = p.getBytes();
            System.arraycopy(bytes, 0, memoria, posicionActual, bytes.length);
            posicionActual += bytes.length;
            memoria[posicionActual++] = 0x00; // Terminador '\0'
        }

        // 2. Escribir punteros (cada puntero = 4 bytes, 0x0000 + offset)
        for (int offsetStr : offsets) {
            memoria[posicionActual++] = 0x00; // 16 bits más significativos = 0x0000
            memoria[posicionActual++] = 0x00;
            memoria[posicionActual++] = (byte) ((offsetStr >> 8) & 0xFF); // 16 bits menos significativos
            memoria[posicionActual++] = (byte) (offsetStr & 0xFF);
            System.out.println( ">>8:"+ ((offsetStr >> 8) & 0xFF) + ":"+(offsetStr & 0xFF));
        }

        // 3. Configurar el segmento en la tabla y el mapa de bases
        int tamanioSegmento = posicionActual-1; // posicionActual apunta a la siguiente posición libre
        tabla.agregarSegmento("PS", (short)0, (short)tamanioSegmento);
        //baseSegmentos.put("PS", 0); // El Param Segment siempre inicia en la dirección física 0x00000000
        
    }

    /*
    public void cargarSegmentoDesdeArchivo(String segmento, FileInputStream fis, int longitud) throws IOException {
        int base = baseSegmentos.get(segmento);
        for (int i = 0; i < longitud; i++) {
            int b = fis.read();
            if (b == -1) throw new IOException("Archivo incompleto al leer segmento " + segmento);
            memoria[base + i] = (byte) b;
        }
    }*/
    
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
        
        DescriptorSegmento segmento = tabla.getSegmento("SS");
        
        if (segmento == null || direccionFisica + 3 > segmento.getBase() + segmento.getTamanio()) {
            throw new IndexOutOfBoundsException("Stack Underflow");
        }

        int valor = 0;
        valor  = (memoria[direccionFisica]     & 0xFF) << 24;
        valor |= (memoria[direccionFisica + 1] & 0xFF) << 16;
        valor |= (memoria[direccionFisica + 2] & 0xFF) << 8;
        valor |= (memoria[direccionFisica + 3] & 0xFF);
        
        return valor;
    }


    public void escribirByte(int direccionFisica,byte valor) {
    	memoria[direccionFisica] = valor;
    }
    public void escribirByteLogica(int direccionLogica, byte valor) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	escribirByte(direccionFisica,valor);
    }
    @Override
	 public byte leerPrimerByte(int dirLogicaIP) {
		int dirFisicaIP = getDireccionFisica(dirLogicaIP); 
		return leerByte(dirFisicaIP);
	 }

    @Override
    public int getDireccionFisica(int direccionLogica) {
        int segmento = (direccionLogica >>> 16) & 0xFFFF; // sin signo
        short signedOffset = (short) (direccionLogica & 0xFFFF); // Los 16 bits inferiores, ahora interpretados como short (con signo)
        int offset = signedOffset;
        if (segmento >= tabla.getCantidadSegmentos()) {
            throw new IllegalArgumentException(
                "Segmento inválido: " + segmento +
                " para dirección lógica: 0x" + Integer.toHexString(direccionLogica)
            );
        }

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
    /*
    public int leerOperando(int direccionLogica, int bytesYaLeidos, int cantidadBytes) {
        int direccionFisica = getDireccionFisica(direccionLogica) + bytesYaLeidos;
        int valor = 0;
        for (int i = 0; i < cantidadBytes; i++) {
            valor = (valor << 8) | (memoria[direccionFisica + i] & 0xFF);
        }
        if (cantidadBytes < 4) {
            int shift = (4 - cantidadBytes) * 8;
            valor = (valor << shift) >> shift;
        }
        return valor;
    }
    */

	public int leerOperando(int direccionLogica, int bytesYaLeidos, int cantidadBytes) {
	    int direccionFisica = getDireccionFisica(direccionLogica);
	    direccionFisica += bytesYaLeidos;

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
        
        //extender el signo 
        if (cantidadBytes < 4) {
            int shift = (4 - cantidadBytes) * 8;
            valor = (valor << shift) >> shift;
        }
        return valor;
    }
    
    /*
     * private int reconstruirValor(int direccionFisica, int cantidadBytes, boolean extenderSigno) {
    int valor = 0;
    for (int i = 0; i < cantidadBytes; i++) {
        valor = (valor << 8) | (memoria[direccionFisica + i] & 0xFF);
    }
    if (extenderSigno && cantidadBytes < 4) {
        int shift = (4 - cantidadBytes) * 8;
        valor = (valor << shift) >> shift;
    }
    return valor;
}

public int leerOperando(int direccionLogica, int bytesYaLeidos, int cantidadBytes) {
    int direccionFisica = getDireccionFisica(direccionLogica) + bytesYaLeidos;
    return reconstruirValor(direccionFisica, cantidadBytes, true);
}

     * */
    
    public void escribirPila(int direccionLogica,int valor) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	if(direccionFisica < tabla.getSegmento("SS").getBase()) {
    		throw new IndexOutOfBoundsException("Stack Overflow");
    	}
    	
    	memoria[direccionFisica]     = (byte) ((valor >> 24) & 0xFF);
        memoria[direccionFisica + 1] = (byte) ((valor >> 16) & 0xFF);
        memoria[direccionFisica + 2] = (byte) ((valor >> 8) & 0xFF);
        memoria[direccionFisica + 3] = (byte) (valor & 0xFF);
    }
    
    public void escribirPila(int direccionLogica, int valor, int cantBytes) {
        int direccionFisica = getDireccionFisica(direccionLogica);

        // Extender signo según cantidad de bytes del valor:
        if (cantBytes == 1) {
            valor = (byte) valor; // extiende signo a 32 bits
        } else if (cantBytes == 2) {
            valor = (short) valor; // extiende signo a 32 bits
        } else if (cantBytes == 3) {
            // Extensión de signo para 3 bytes (24 bits)
            if ((valor & 0x800000) != 0) { // si el bit 23 es 1 (signo negativo)
                valor |= 0xFF000000; // pone los 8 bits más altos a 1 para extender signo
            } else {
                valor &= 0x00FFFFFF; // limpia los 8 bits más altos
            }
        }
        if(direccionFisica < tabla.getSegmento("SS").getBase()) {
    		throw new IndexOutOfBoundsException("Stack Overflow");
    	}
        
        memoria[direccionFisica]     = (byte) ((valor >> 24) & 0xFF);
        memoria[direccionFisica + 1] = (byte) ((valor >> 16) & 0xFF);
        memoria[direccionFisica + 2] = (byte) ((valor >> 8) & 0xFF);
        memoria[direccionFisica + 3] = (byte) (valor & 0xFF);
    }


    
    @Override
    public void escribirOperando(int direccionLogica, int valor, int cantBytes) {

        int direccionFisica = getDireccionFisica(direccionLogica);

        if (direccionFisica < 0 || direccionFisica + 4 > tamanoMemoria) {
            throw new IllegalArgumentException("Direccion fuera de los limites de la memoria: " + direccionFisica);
        }


        // Escribir desde la direccion física hacia adelante
        for (int i = 0; i < cantBytes; i++) {
            int byteActual = (valor >> ((cantBytes - 1 - i) * 8)) & 0xFF;
            memoria[direccionFisica + i] = (byte) byteActual;
        }
    }
    /*
    public void escribirOperando(int direccionLogica, int valor,int cantBytes) {
        int direccionFisica = getDireccionFisica(direccionLogica);
        
	    if (direccionFisica < 0 || direccionFisica + cantBytes >= tamanoMemoria) {
	        throw new IllegalArgumentException("Direccion fuera de los limites de la memoria: " + direccionFisica);
	    }
	    //System.out.println("cantBytes"+ cantBytes);
        for (int i = 0; i < cantBytes; i++) {
        	int byteActual = ((valor >> ((4 - 1 - i) * 8)) & 0xFF);
            memoria[direccionFisica + i] = (byte)byteActual;
            //System.out.println("escribiendo en "+ (direccionFisica+i)+": "+ (byte)byteActual);
        }
    }
    */
	public void escribirOperando(int direccionLogica, int valor) {
		
	    int direccionFisica = getDireccionFisica(direccionLogica);
	    
	    // Validar que se disponga de 4 bytes a partir de la direccion fisica en memoria
	    if (direccionFisica < 0 || direccionFisica + 3 >= tamanoMemoria) {
	        throw new IllegalArgumentException("Direccion fuera de los limites de la memoria: " + direccionFisica);
	    }
	    
	    // Escribir el valor en memoria
	    memoria[direccionFisica]     = (byte) ((valor >> 24) & 0xFF);
	    memoria[direccionFisica + 1] = (byte) ((valor >> 16) & 0xFF);
	    memoria[direccionFisica + 2] = (byte) ((valor >> 8) & 0xFF);
	    memoria[direccionFisica + 3] = (byte) (valor & 0xFF);
	}

    /*
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
        memoria[direccionFisica + 3] = (byte) (valor & 0xFF);
    }
        */
     
    
    public void imprimirMemoria(int direccionLogica, int offset) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	for(int i=direccionFisica; i<direccionFisica + offset;i++)
    		System.out.println(tabla.getSegmentoDirFisica(i) + ": " +Integer.toHexString(i) + ": "+ Integer.toHexString(memoria[i] & 0xFF));
    }
    public int getTamano() {
    	return this.tamanoMemoria;
    }
}
