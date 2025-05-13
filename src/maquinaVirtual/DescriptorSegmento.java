package maquinaVirtual;

public class DescriptorSegmento {
	private String nombre;
	private short base,limite;
	private int tamanio;
	
	public DescriptorSegmento(String nombre, int tamanio) {
		this.nombre = nombre;
		this.tamanio = tamanio;
	}
	
	public DescriptorSegmento(String nombre, short base,short limite) {
		this.base = base;
		this.limite = limite;
		this.nombre = nombre;
	}

	
	public String getNombre() {
		return nombre;
	}


	public void seTamanio(int tamanio) {
		this.tamanio = tamanio;
	}
	public int getTamanio() {
		return this.tamanio;
	}
	
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public short getBase() {
		return base;
	}

	public void setBase(short base) {
		this.base = base;
	}

	public short getLimite() {
		return limite;
	}

	public void setLimite(short limite) {
		this.limite= limite;
	}

	@Override
	public String toString() {
		return "DescriptorSegmento [nombre=" + nombre + ", base=" + base + ", limite=" + limite + ", tamanio=" + tamanio
				+ "]";
	}

		

}
