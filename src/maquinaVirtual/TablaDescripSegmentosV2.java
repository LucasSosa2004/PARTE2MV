package maquinaVirtual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;


public class TablaDescripSegmentosV2 {
    private final ArrayList<DescriptorSegmento> tabla;
    private int entryPoint;

    public TablaDescripSegmentosV2(HeaderMV header) {
        this.tabla = new ArrayList<DescriptorSegmento>();
        this.entryPoint = header.getEntryPoint();
    }

    public TablaDescripSegmentosV2() {
        this.tabla = new ArrayList<DescriptorSegmento>();
        this.entryPoint=0;
    }

    public int getBase(int index) {
        if (index < 0 || index >= tabla.size()) {
            throw new IndexOutOfBoundsException("Indice invalido en tabla de descriptores: " + index);
        }
        return tabla.get(index).getBase();
    }


    public int getLimite(int index) {
        if (index < 0 || index >= tabla.size()) {
            throw new IndexOutOfBoundsException("Indice invalido en tabla de descriptores: " + index);
        }
        return tabla.get(index).getTamanio();
    }

    public String getNombreSegmento(int index) {
        switch (index) {
            case 0: return "PS";
            case 1: return "CS";
            case 2: return "DS";
            case 3: return "ES";
            case 4: return "SS";
            case 5: return "KS";
            default: return "???";
        }
    }    
    public String getNombreSegmentoVMI(int index) {
        switch (index) {
        case 0: return "PS";
        case 1: return "KS";
        case 2: return "CS";
        case 3: return "DS";
        case 4: return "ES";
        case 5: return "SS";
        default: return "???";
    }
    }
    
    public void agregarSegmento(String segmento, short base, short limite) {
        tabla.add(new DescriptorSegmento(segmento,base,limite));
    }

    public void setTabla(HeaderMV header) {
        ArrayList<DescriptorSegmento> segmentos = header.getSegmentos();
        //HashMap<String, DescriptorSegmento> segmentosMap = new HashMap<>();

        
        for(DescriptorSegmento segmento : segmentos) {
        	if (segmento != null && segmento.getTamanio()>0) {
                short base = 0;
                if(!(tabla.isEmpty())) {
                    DescriptorSegmento anterior = tabla.getLast();    				
                    base = (short)(anterior.getBase() + anterior.getTamanio() +  1);    				
                }

                agregarSegmento(segmento.getNombre(), base, segmento.getTamanio());
            }
        }
        mostrarTabla();
        
        /*
        // Primero guardamos todos los segmentos en un mapa
        for (DescriptorSegmento segmento : segmentos) {
            if(segmento.getTamanio() > 0) {
                segmentosMap.put(segmento.getNombre(), segmento);
            }
        }
        

        // Definimos el orden deseado
        String[] ordenSegmentos = {"PS", "KS", "CS", "DS", "ES", "SS"};

        // Cargamos los segmentos en el orden especificado
        for (String nombreSegmento : ordenSegmentos) {
            DescriptorSegmento segmento = segmentosMap.get(nombreSegmento);
            if (segmento != null) {
                short base = 0;
                if(!(tabla.isEmpty())) {
                    DescriptorSegmento anterior = tabla.getLast();    				
                    base = (short)(anterior.getBase() + anterior.getTamanio() +  1);    				
                }
                short limite = (short)(segmento.getTamanio());
                agregarSegmento(segmento.getNombre(), base, limite);
            }
        }*/
    }
    
    public DescriptorSegmento getSegmento(String segmento) {
    	int i = 0;
    	while(i < this.tabla.size() && !(this.tabla.get(i).getNombre().equals(segmento))) 
    		i++;
    	if(i < this.tabla.size())
    		return this.tabla.get(i);
    	else 
    		return null;
    }
    
    public DescriptorSegmento getSegmento(int index) {
        if (index < 0 || index >= tabla.size()) {
            return null;
        }
        return tabla.get(index);
    }

    public int inincializarRegistro(String registro) {
		DescriptorSegmento segmento = getSegmento(registro);

			
		if(segmento == null) 
			return -1;
		else {
			System.out.println(registro + getIndice(registro));
			short base = (short) (getIndice(registro));
			return (int) base << 16;
		}
	}
    
    public int inicializarSP() {	
    	int SS = getIndice("SS");
    	if(SS>0) {
    		int offset = getSegmento("SS").getTamanio();
    		return ((SS << 16) | offset) +1;    		
    	}
    	else return -1;
    }
    
    
    public void mostrarTabla() {

    	for(DescriptorSegmento i: tabla) {
    		System.out.println(i.getNombre() + " : "+ i.getBase() + " - " +i.getTamanio());
    	}
    }
    
    //se tiene que usar en un segmento siguiente al PS
    public DescriptorSegmento getAnterior(String segmento) {
    	try {
    		int i = getIndice(segmento);
    		return this.tabla.get(i-1);
    	}
    	catch(NoSuchElementException e) {
    		throw new NoSuchElementException("Es el primer segmento");
    	}
    }
    
    public int getIndice(String segmento) {
    	return tabla.indexOf(getSegmento(segmento));
    }


    
    public int getCantidadSegmentos() {
        return tabla.size();
    }
    
    public ArrayList<DescriptorSegmento> getSegmentos(){
    	return this.tabla;
    }
    public int getBaseFisica(int segmento) {
    	return getBase(segmento);
    }
    
    public String getSegmentoDirFisica(int direccionFisica) {
        int i = 0;
        boolean encontrado = false;

        while (i < tabla.size() && !encontrado) {
            DescriptorSegmento seg = tabla.get(i);
            if (direccionFisica >= seg.getBase() && direccionFisica < seg.getBase() + seg.getTamanio() + 1) {
                encontrado = true;
            } else {
                i++;
            }
        }

        if (encontrado) {
            return tabla.get(i).getNombre();
        } else {
            return "Sin segmento"; // No se encontró ningún segmento
        }
    }

    public int getEntryPoint() {
    	return this.entryPoint;
    }


} 
