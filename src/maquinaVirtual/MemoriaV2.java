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
        	cargarParametros(parametros,0); 
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
        
        
        int posicionActual = inicioStrings;
        for (String p : parametros) {
            
            offsets.add(posicionActual);
            
            byte[] bytes = p.getBytes();
            System.arraycopy(bytes, 0, memoria, posicionActual, bytes.length);
            posicionActual += bytes.length;
            memoria[posicionActual++] = 0x00; 
        }

       
        for (int offsetStr : offsets) {
            memoria[posicionActual++] = 0x00; 
            memoria[posicionActual++] = 0x00;
            memoria[posicionActual++] = (byte) ((offsetStr >> 8) & 0xFF); 
            memoria[posicionActual++] = (byte) (offsetStr & 0xFF);
            
        }

        
        int tamanioSegmento = posicionActual-1; 
        tabla.agregarSegmento("PS", (short)0, (short)tamanioSegmento);
        
        
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
        int segmento = (direccionLogica >>> 16) & 0xFFFF; 
        short signedOffset = (short) (direccionLogica & 0xFFFF); 
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
	    int offset = (direccionLogica & 0xFFFF) + offsetAdicional; 
	    offset &= 0xFFFF; 
	    int segmento = direccionLogica & 0xFFFF0000; 
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
        
        
        if (cantidadBytes < 4) {
            int shift = (4 - cantidadBytes) * 8;
            valor = (valor << shift) >> shift;
        }
        return valor;
    }
    
   
    
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

        
        if (cantBytes == 1) {
            valor = (byte) valor; 
        } else if (cantBytes == 2) {
            valor = (short) valor; 
        } else if (cantBytes == 3) {
            
            if ((valor & 0x800000) != 0) { 
                valor |= 0xFF000000; 
            } else {
                valor &= 0x00FFFFFF; 
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


        
        for (int i = 0; i < cantBytes; i++) {
            int byteActual = (valor >> ((cantBytes - 1 - i) * 8)) & 0xFF;
            memoria[direccionFisica + i] = (byte) byteActual;
        }
    }


	public void escribirOperando(int direccionLogica, int valor) {
		
	    int direccionFisica = getDireccionFisica(direccionLogica);
	    
	   
	    if (direccionFisica < 0 || direccionFisica + 3 >= tamanoMemoria) {
	        throw new IllegalArgumentException("Direccion fuera de los limites de la memoria: " + direccionFisica);
	    }
	    
	    
	    memoria[direccionFisica]     = (byte) ((valor >> 24) & 0xFF);
	    memoria[direccionFisica + 1] = (byte) ((valor >> 16) & 0xFF);
	    memoria[direccionFisica + 2] = (byte) ((valor >> 8) & 0xFF);
	    memoria[direccionFisica + 3] = (byte) (valor & 0xFF);
	}

   
     
    
    public void imprimirMemoria(int direccionLogica, int offset) {
    	int direccionFisica = getDireccionFisica(direccionLogica);
    	
    }
    public int getTamano() {
    	return this.tamanoMemoria;
    }
}
