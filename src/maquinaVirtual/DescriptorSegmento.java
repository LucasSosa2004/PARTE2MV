package maquinaVirtual;

public class DescriptorSegmento {
	private String nombre;
	private short base;
	private short tamanio;
	
	public DescriptorSegmento(String nombre, short tamanio) {
		this.nombre = nombre;
		this.tamanio = tamanio;
	}
	
	public DescriptorSegmento(String nombre, short base,short tamanio) {
		this.base = base;
		this.nombre = nombre;
		this.tamanio = tamanio;
	}

	
	public String getNombre() {
		return nombre;
	}


	public void setTamanio(short tamanio) {
		this.tamanio = tamanio;
	}
	public short getTamanio() {
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


	@Override
	public String toString() {
		return "DescriptorSegmento [nombre=" + nombre + ", base=" + base + ", tamanio=" + tamanio
				+ "]";
	}

		

}
