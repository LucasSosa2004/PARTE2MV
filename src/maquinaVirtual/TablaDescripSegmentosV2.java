package maquinaVirtual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TablaDescripSegmentosV2 {
    private final List<DescriptorSegmento> tabla;

    public TablaDescripSegmentosV2() {
        this.tabla = new ArrayList<>();
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


    public void agregarSegmento(String segmento, short base, short limite) {
        tabla.add(new DescriptorSegmento(segmento,base,limite));
    }

    public void setTabla(HeaderMV header) {
    	HashMap<String,Integer> segmentos = header.getSegmentos();
    	for (HashMap.Entry<String, Integer> segmento : segmentos.entrySet()) { //clave y valor
    		if(segmento.getValue()>0) {
    			short base = 0;
    			if(!(tabla.isEmpty())) {
    				DescriptorSegmento anterior = tabla.getLast();    				
    				base = (short)(anterior.getLimite() + 1);    				
    			}
    			short limite = (short)(base + segmento.getValue());
    			agregarSegmento(segmento.getKey(),base,limite);
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
    	return this.tabla.get(index);
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
    	DescriptorSegmento SS = getSegmento("SS");
    	if(SS == null)
    		return -1;
    	else
    		return getIndice("SS") + SS.getTamanio();
    }
    
    
    public void mostrarTabla() {
    	for(DescriptorSegmento i: tabla) {
    		System.out.println(i.getNombre() + " : "+ i.getBase() + " - " +i.getLimite());
    	}
    }
    
    public int getIndice(String segmento) {
    	return tabla.indexOf(getSegmento(segmento));
    }
    
    public int getCantidadSegmentos() {
        return tabla.size();
    }


} 
