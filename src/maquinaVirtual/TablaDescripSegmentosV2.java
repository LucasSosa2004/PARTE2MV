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
        return tabla.get(index).getLimite();
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
    
    public void agregarSegmento(String segmento, short base, short limite) {
        tabla.add(new DescriptorSegmento(segmento,base,limite));
    }

    public void setTabla(HeaderMV header) {
    	ArrayList<DescriptorSegmento> segmentos = header.getSegmentos();
    	for (DescriptorSegmento segmento : segmentos) { //clave y valor
    		if(segmento.getTamanio()>0) {
    			short base = 0;
    			if(!(tabla.isEmpty())) {
    				DescriptorSegmento anterior = tabla.getLast();    				
    				base = (short)(anterior.getLimite() + 1);    				
    			}
    			short limite = (short)(base + segmento.getTamanio());
    			agregarSegmento(segmento.getNombre(),base,limite);
    		}
    	}
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
			short base = (short) getIndice(registro);
			return (int) base << 16;
		}
	}
    
    public int inicializarSP() {	
    	int SS = getIndice("SS");
    	int offset = getSegmento("SS").getTamanio();
    	return ((SS << 16) | offset) + 1;
    }
    
    
    public void mostrarTabla() {

    	for(DescriptorSegmento i: tabla) {
    		System.out.println(i.getNombre() + " : "+ i.getBase() + " - " +i.getLimite());
    	}
    }
    
    //se tiene que usar en un segmento siguiente al code segment
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
