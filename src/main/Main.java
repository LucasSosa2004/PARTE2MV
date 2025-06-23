package main;

import java.io.FileInputStream;

import maquinaVirtual.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import maquinaVirtual.HeaderMV;
import maquinaVirtual.MaquinaVirtual;

public class Main {
    public static void main(String[] args) {
    	int memoriaKiB = 16; // valor por defecto		
        boolean disassemblerMode = false;
        List<String> parametrosPrograma = new ArrayList<>();
        
        Archivos archivos = Archivos.desdeArgs(args);

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("m=")) {
                try {
                    memoriaKiB = Integer.parseInt(arg.substring(2));
                } catch (NumberFormatException e) {
                    System.err.println("Error: el valor de memoria no es válido.");
                    return;
                }

            } else if (arg.equals("-d")) {
                disassemblerMode = true;
            } else if (arg.equals("-p") && archivos.tieneVMX()) {
                for (int j = i + 1; j < args.length; j++) {
                    parametrosPrograma.add(args[j]);
                }
                break;
            }
        }

        if (!archivos.tieneVMX() && !archivos.tieneVMI()) {
            System.err.println("Error: se debe especificar al menos un archivo .vmx o .vmi");
            return;
        }

        try {
        	

            MaquinaVirtual MV;

            if(archivos.tieneVMX()) {
            	
            	FileInputStream fis = new FileInputStream(archivos.getVMX());
            	HeaderMV header = leerHeader(fis);
            	
            	if (!header.isValido()) {
            		System.err.println("Archivo invalido. Identificador incorrecto: " + header.getIdentificador());
            		fis.close();
            		return;
            	}
            	
	            if (header.getVersion() == 1) {
	                MV = new MaquinaVirtual(header.getTamanoCodigov1(), false);
	                cargarCodigo(fis, MV, header.getTamanoCodigov1());
	                if (disassemblerMode) {
	                    ejecutarDisassembler(MV, header.getTamanoCodigov1());
	                }
	            } else if (header.getVersion() == 2) {

	                int tamMemoria = memoriaKiB * 1024;
	                MV = new MaquinaVirtual(header, parametrosPrograma, tamMemoria,archivos,false); // usa MemoriaV2 internamente

	                DisassemblerV2 disV2 = new DisassemblerV2(MV.getRegistros(),MV.getMemoria(),MV.getTabla());
	                cargarCodigo(fis,MV);
                    if (disassemblerMode) {
	                    disV2.mostrarCadenas();  // Primero mostrar cadenas
	                    disV2.disassembleAll();  // Luego mostrar instrucciones
	                    
	                    // Restaurar IP al entry point después del disassembler
	                    int entryPoint = MV.getTabla().getSegmento("CS").getBase() + header.getEntryPoint();
	                    MV.getRegistros().setIP(entryPoint);
	                }
                    
	            } else {
	                System.err.println("Version de VMX no soportada.");
	                fis.close();
	                return;
	            }
	            
	            if(disassemblerMode) {
	            	int tamanoCodigo = header.getTamanoCodigov1();
	            	//ejecutarDisassembler(MV,tamanoCodigo);
	            }
	            if (!archivos.tieneVMI()){
	            	MV.getTabla().mostrarTabla();
	            	System.out.println(Integer.toHexString(MV.getRegistros().getSP()));
	            	//MV.getMemoria().imprimirMemoria(MV.getTabla().getSegmento("CS").getBase(),485);
	            	ejecutarPrograma(MV);
	            }
	            else{
	            	ejecutarEnDebug(MV, archivos);
	            }
	            
	            	
	            	
	            fis.close();
            }
            else { // no vmx pero si vmi
            	int tamMemoria = memoriaKiB * 1024;
            	MV = new MaquinaVirtual(parametrosPrograma,tamMemoria,archivos,false);
            	if (archivos.tieneVMI()) {
                    archivos.cargarEnMV(MV); 
                    MV.getTabla().mostrarTabla(); 
                    MV.getRegistros().mostrarRegistros();
                }

                if (disassemblerMode) {
                	int tamanoCodigo = MV.getTabla().getSegmento("CS").getBase();
            		ejecutarDisassembler(MV, tamanoCodigo);
                } else {
                    ejecutarPrograma(MV);
                }

            }
            
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static HeaderMV leerHeader(FileInputStream fis) throws IOException {
        byte[] baseHeader = new byte[6];
        if (fis.read(baseHeader) != baseHeader.length) {
            throw new IOException("No se pudieron leer los primeros 6 bytes del header.");
        }

        String identificador = new String(baseHeader, 0, 5);
        int version = baseHeader[5] & 0xFF;

        HeaderMV header = new HeaderMV(identificador, version);

        switch (identificador) {
            case "VMX25":
                if (version == 1) {
                    byte[] v1Extra = new byte[2]; // bytes 6-7
                    if (fis.read(v1Extra) != v1Extra.length) {
                        throw new IOException("Header .vmx v1 incompleto.");
                    }
                    int tamanoCodigo = ((v1Extra[0] & 0xFF) << 8) | (v1Extra[1] & 0xFF);
                    header.setTamanoCodigov1(tamanoCodigo);
                    //System.out.println("tamano codigo = " + tamanoCodigo);
                } else if (version == 2) {
                    byte[] v2Extra = new byte[12]; // bytes 6-17
                    if (fis.read(v2Extra) != v2Extra.length) {
                        throw new IOException("Header .vmx v2 incompleto.");
                    }
                    header.agregarSegmento("KS", mascara2bytes(v2Extra[8], v2Extra[9]));
                    header.agregarSegmento("CS", mascara2bytes(v2Extra[0], v2Extra[1]));
                    header.agregarSegmento("DS", mascara2bytes(v2Extra[2], v2Extra[3]));
                    header.agregarSegmento("ES", mascara2bytes(v2Extra[4], v2Extra[5]));
                    header.agregarSegmento("SS", mascara2bytes(v2Extra[6], v2Extra[7]));
                    header.setEntryPoint(mascara2bytes(v2Extra[10], v2Extra[11]));
                } else {
                    throw new IOException("Version de .vmx no soportada: " + version);
                }
                break;

            case "VMI25":
                if (version != 1) {
                    throw new IOException("Version de .vmi no soportada: " + version);
                }
                byte[] vmiExtra = new byte[2]; // bytes 6-7
                if (fis.read(vmiExtra) != vmiExtra.length) {
                    throw new IOException("Header .vmi incompleto.");
                }
                int memoria = ((vmiExtra[0] & 0xFF) << 8) | (vmiExtra[1] & 0xFF);
                header.setMemoriaKiB(memoria);
                break;

            default:
                throw new IOException("Identificador desconocido: " + identificador);
        }

        return header;
    }
    
    private static void cargarCodigo(FileInputStream fis, MaquinaVirtual MV, int tamanoCodigo) throws IOException { //en V2 se llama con el tamano del CS
        int bytesLeidos = 0; //TODO
        int byteLeido;
        while (bytesLeidos < tamanoCodigo && (byteLeido = fis.read()) != -1) {
            MV.getMemoria().cargarByteAMemoria((byte) byteLeido, bytesLeidos);
            bytesLeidos++;
        }
    }
    private static void cargarCodigo(FileInputStream fis, MaquinaVirtual MV) throws IOException { //en V2 se llama con el tamano del CS
        int ptrCS = MV.getMemoria().getDireccionFisica(MV.getRegistros().getCS()); //TODO
        int byteLeido;
        int limite = MV.getTabla().getSegmento("CS").getTamanio() + MV.getTabla().getSegmento("CS").getBase() ; 
        while (ptrCS <= limite && (byteLeido = fis.read()) != -1) {
            MV.getMemoria().cargarByteAMemoria((byte)byteLeido, ptrCS);
            ptrCS++;
        }
        
        // Cargar contenido del KS
        if (MV.getTabla().getSegmento("KS") != null && MV.getTabla().getSegmento("KS").getTamanio() > 0) {
            int ptrKS = MV.getTabla().getSegmento("KS").getBase();
            int limiteKS = MV.getTabla().getSegmento("KS").getTamanio() + MV.getTabla().getSegmento("KS").getBase();
            while (ptrKS < limiteKS && (byteLeido = fis.read()) != -1) {
                MV.getMemoria().cargarByteAMemoria((byte)byteLeido, ptrKS);
                ptrKS++;
            }
        }
    }
   
    
    private static int mascara2bytes(byte pri, byte seg) {
    	return ((pri & 0xFF) << 8) | (seg & 0xFF);    	
    }
    
    private static void ejecutarDisassembler(MaquinaVirtual MV, HeaderMV header) {
        int i = 0, bytesInstruccion = 1;
        while (i < header.getTamanoCodigov1() && bytesInstruccion != -1) {
            int IP = MV.getRegistros().getIP();
            byte primerByte = MV.getMemoria().leerPrimerByte(IP);
            bytesInstruccion = MV.getDissasemblerAux().decodificarInstruccion(primerByte);
            if (bytesInstruccion > 0) {
                MV.getRegistros().modificaIP(bytesInstruccion);
                i += bytesInstruccion;
            }
        }
    } 
    private static void ejecutarDisassembler(MaquinaVirtual MV, int tamanoCodigo) {
        int i = 0, bytesInstruccion = 1;
        while (i < tamanoCodigo && bytesInstruccion != -1) {
            int IP = MV.getRegistros().getIP();
            byte primerByte = MV.getMemoria().leerPrimerByte(IP);
            bytesInstruccion = MV.getDissasemblerAux().decodificarInstruccion(primerByte);
            if (bytesInstruccion > 0) {
                MV.getRegistros().modificaIP(bytesInstruccion);
                i += bytesInstruccion;
            }
        }
    } 
    
    private static void ejecutarPrograma(MaquinaVirtual MV) {
        int bytesInstruccion = 1;
        boolean IPcayoSegm = false;
        while (!IPcayoSegm && bytesInstruccion != -1) {
        	int IP = MV.getRegistros().getIP();
        	System.out.println(Integer.toHexString(IP+0x1d));
            byte primerByte = MV.getMemoria().leerPrimerByte(IP);
            bytesInstruccion = MV.getUnidadAritmeticoLogica().ejecutarInstruccion(primerByte);
            IPcayoSegm = MV.caidaSegmentoIP();
        }
        if (IPcayoSegm) {
            System.out.println("ERROR: Ejecucion interrumpida por caida de segmento del IP");
        }
    }
    
    private static void ejecutarEnDebug(MaquinaVirtual MV, Archivos archivos) throws IOException {
        Scanner sc = new Scanner(System.in);
        int bytesInstruccion = 1;
        boolean IPcayoSegm = false;
        boolean esperaInput = false;  // arranca ejecutando sin pausar
        boolean ejecutar = true;

        int IP;
        byte primerByte;

        System.out.println("debug");

        while (!IPcayoSegm && bytesInstruccion != -1) {
            ejecutar = true;

            if (esperaInput || MV.getUnidadAritmeticoLogica().getBreakPointAnterior()) {
                System.out.print(">>> ");
                String input = sc.nextLine();

                if (input.equals("q")) {
                    System.out.println("Ejecución finalizada.");
                    break;
                } else if (input.isEmpty()) { // Step
                    esperaInput = true;
                    archivos.guardarArchivoVMI(MV.getRegistros(), MV.getMemoria(), MV.getTabla());
                } else if (input.equals("d")) { // Continuar
                    esperaInput = false;
                } else {
                    System.out.println("Enter (step), d (continuar) o q (salir).");
                    ejecutar = false;
                }
            }
            if (ejecutar) {
            	IP = MV.getRegistros().getIP();
            	primerByte = MV.getMemoria().leerPrimerByte(IP);
            	bytesInstruccion = MV.getUnidadAritmeticoLogica().ejecutarInstruccion(primerByte);
            	IPcayoSegm = MV.caidaSegmentoIP();
            	
            	// breakpoint despues de >>d
            	if (!esperaInput && MV.getUnidadAritmeticoLogica().getBreakPointAnterior()) {
            		esperaInput = true;
            	}
            }

        }

        if (IPcayoSegm) {
            System.out.println("ERROR: Ejecución interrumpida por caída de segmento del IP");
        }
    }


    

    public static String formatoBinario(int valor) {
        String binario = String.format("%32s", Integer.toBinaryString(valor)).replace(' ', '0');
        return binario.replaceAll("(.{8})(?=.)", "$1 ");
    }
    
}
