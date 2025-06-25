package maquinaVirtual;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class DisassemblerV2 {
    private Registros registros;
    private MemoriaBase memoria;
    private TablaDescripSegmentosV2 tabla;
    private int entryPoint;

    public DisassemblerV2(Registros registros, MemoriaBase memoria, TablaDescripSegmentosV2 tabla) {
        this.registros = registros;
        this.memoria = memoria;
        this.tabla = tabla;
        
        DescriptorSegmento segmentoCS = tabla.getSegmento("CS");
        if (segmentoCS != null) {
            this.entryPoint = segmentoCS.getBase() + tabla.getEntryPoint();
        } else {
            this.entryPoint = tabla.getEntryPoint();
        }
    }

    public int decodificarInstruccion(byte primerByte) {
        byte codOperacion = (byte) (primerByte & 0x1F);
        int bytesYaLeidosInstruccion = 1; 

        byte tipoOpA = 0, tipoOpB = 0;
        int valorOpA = 0, valorOpB = 0;
        int cantBytesOpA = 0, cantBytesOpB = 0;
        String instHexa = ""; 
        String lineaDissasembler = "";

        int ipFisica = registros.getIP();
        String segmentoActual = tabla.getSegmentoDirFisica(ipFisica);
        if (segmentoActual == null) {
            throw new IllegalStateException("No se encontró segmento para IP física: " + ipFisica);
        }
        int dirBase = tabla.getSegmento(segmentoActual).getBase();
        int offset = registros.getIP() - dirBase;
        
        if (codOperacion <= 0x0D) {  
            tipoOpA = (byte) ((primerByte >> 6) & 0x03);

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion); 
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;

            instHexa = concatenarInstHexaUnOperando(primerByte, valorOpA, cantBytesOpA);
            lineaDissasembler = String.format("%s %s",
                    codOpAMnemonico(codOperacion),
                    decodificarOp(registros, tipoOpA, valorOpA));
        } 
        else if (codOperacion == 0x0B || codOperacion == 0x0C) { 
            tipoOpA = (byte) ((primerByte >> 6) & 0x03);

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion); 
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;

            instHexa = concatenarInstHexaUnOperando(primerByte, valorOpA, cantBytesOpA);
            lineaDissasembler = String.format("%s %s",
                    codOpAMnemonico(codOperacion),
                    decodificarOp(registros, tipoOpA, valorOpA));
        }
        else if (codOperacion >= 0x10 && codOperacion <= 0x1E) { 
            tipoOpB = (byte) ((primerByte & 0xC0) >> 6);
            tipoOpA = (byte) ((primerByte & 0x30) >> 4);

            valorOpB = obtenerOpEnMemoria(tipoOpB, bytesYaLeidosInstruccion);
            cantBytesOpB = cantidadBytesOperando(tipoOpB);
            bytesYaLeidosInstruccion += cantBytesOpB;

            valorOpA = obtenerOpEnMemoria(tipoOpA, bytesYaLeidosInstruccion);
            cantBytesOpA = cantidadBytesOperando(tipoOpA);
            bytesYaLeidosInstruccion += cantBytesOpA;

            instHexa = concatenarInstHexaDosOperandos(primerByte, valorOpB, cantBytesOpB, valorOpA, cantBytesOpA);
            lineaDissasembler = String.format("%s %s, %s",
                    codOpAMnemonico(codOperacion),
                    decodificarOp(registros, tipoOpA, valorOpA),
                    decodificarOp(registros, tipoOpB, valorOpB));
        } 
        else if (codOperacion == 0x0F) { 
            instHexa = "0F";
            lineaDissasembler = "STOP";
            String entryPointMark = (registros.getIP() == entryPoint) ? "> " : "  ";
            System.out.printf("%s[%s:%04X] %-20s | %s\n", 
                    entryPointMark,
                    segmentoActual, 
                    registros.getIP(), 
                    instHexa, 
                    lineaDissasembler);
            return -1; 
        }
        else if (codOperacion == 0x0E) { 
            instHexa = "0E";
            lineaDissasembler = "RET";
        }
        else {
            bytesYaLeidosInstruccion = -1; 
        }

        
        String entryPointMark = (registros.getIP() == entryPoint) ? "> " : "  ";
        System.out.printf("%s[%04X] %-20s | %s\n", 
                entryPointMark,
                registros.getIP(), 
                instHexa, 
                lineaDissasembler);

        return bytesYaLeidosInstruccion;
    }

    public void mostrarCadenas() {
        DescriptorSegmento segmentoKS = tabla.getSegmento("KS");
        if (segmentoKS == null) return;

        int base = segmentoKS.getBase();
        int limite = base + segmentoKS.getTamanio();
        int pos = base;

        while (pos < limite) {
            
            while (pos < limite && memoria.leerByte(pos) == 0) {
                pos++;
            }
            if (pos >= limite) break;

            int inicioCadena = pos;
            StringBuilder hexString = new StringBuilder();
            StringBuilder asciiString = new StringBuilder();
            boolean cadenaCompleta = false;
            int bytesLeidos = 0;

            
            while (pos < limite && !cadenaCompleta) {
                byte b = memoria.leerByte(pos);
                hexString.append(String.format("%02X ", b & 0xFF));
                bytesLeidos++;

                
                if (b >= 32 && b <= 126) {
                    asciiString.append((char)b);
                } else {
                    asciiString.append('.');
                }

                if (b == 0) {
                    cadenaCompleta = true;
                }
                pos++;
            }

           
            String hexOutput = hexString.toString().trim();
            if (bytesLeidos > 7) {
                
                hexOutput = hexOutput.substring(0, 18) + " ..";
            }

            
            String entryPointMark = "  ";

            int offsetCadena = inicioCadena - base;
            System.out.printf("%s[%04X] %-20s \"%s\"\n", 
                    entryPointMark,
                    offsetCadena,
                    hexOutput,
                    asciiString.toString());
        }
    }

    private int obtenerOpEnMemoria(byte tipoOperando, int bytesYaLeidosInstruccion) {
        int comienzoInstruccion = registros.getIP();

        switch (tipoOperando) {
            case 0b01: 
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 1);
            case 0b10: 
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 2);
            case 0b11: 
                return memoria.leerOperando(comienzoInstruccion, bytesYaLeidosInstruccion, 3);
            default:
                return 0;
        }
    }

    private int cantidadBytesOperando(byte tipoOperando) {
        switch (tipoOperando) {
            case 0b00: return 0;  
            case 0b01: return 1;  
            case 0b10: return 2;  
            case 0b11: return 3;  
            default: return 0;
        }
    }

    public String codOpAMnemonico(byte opcode) {
        switch (opcode) {
            case 0x0:  return "SYS";
            case 0x1:  return "JMP";
            case 0x2:  return "JZ";
            case 0x3:  return "JP";
            case 0x4:  return "JN";
            case 0x5:  return "JNZ";
            case 0x6:  return "JNP";
            case 0x7:  return "JNN";
            case 0x8:  return "NOT";
            case 0x0B: return "PUSH";
            case 0x0C: return "POP";
            case 0x0D: return "CALL";
            case 0x0E: return "RET";
            case 0x0F: return "STOP";
            case 0x10: return "MOV";
            case 0x11: return "ADD";
            case 0x12: return "SUB";
            case 0x13: return "SWAP";
            case 0x14: return "MUL";
            case 0x15: return "DIV";
            case 0x16: return "CMP";
            case 0x17: return "SHL";
            case 0x18: return "SHR";
            case 0x19: return "AND";
            case 0x1A: return "OR";
            case 0x1B: return "XOR";
            case 0x1C: return "LDL";
            case 0x1D: return "LDH";
            case 0x1E: return "RND";
            default: return "UNK";
        }
    }

    public String decodificarOp(Registros registros, byte tipoOp, int operando) {
        switch (tipoOp) {
            case 0b01: 
                int aux = operando >> 4 & 0xF;
                String nombreRegCompleto = registros.getNombreRegistro(aux);
                int sector = (operando >> 2) & 0x3;
                if (sector == 0) {
                    return nombreRegCompleto;
                }
                char medio = nombreRegCompleto.charAt(1);
                String[] SUFIJOS = { "", "L", "H", "X" };
                return medio + SUFIJOS[sector];

            case 0b10: 
                short inmediatoConSigno = (short) operando;
                return String.valueOf((int) inmediatoConSigno);

            case 0b11: 
                
                int tamanoCelda = operando & 0x3;
                String prefijo;
                switch (tamanoCelda) {
                    case 0b00: prefijo = "l"; break; 
                    case 0b10: prefijo = "w"; break; 
                    case 0b11: prefijo = "b"; break; 
                    default: prefijo = ""; break;
                }
                
                
                int codReg = (operando >> 4) & 0xF;
                String nombreReg = registros.getNombreRegistro(codReg);
                
                
                short offsetRaw = (short) ((operando >> 8) & 0xFFFF);
                int offsetConSigno = offsetRaw;
                
                if (offsetConSigno == 0) {
                    return prefijo + "[" + nombreReg + "]";
                } else {
                    return prefijo + "[" + nombreReg + " + " + offsetConSigno + "]";
                }

            default: return "";
        }
    }

    private String concatenarInstHexaUnOperando(byte primerByte, int operando, int cantBytesOpA) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X", primerByte & 0xFF));
        
        
        for (int i = 0; i < cantBytesOpA; i++) {
            sb.append(String.format(" %02X", (operando >> (i * 8)) & 0xFF));
        }
        
        return sb.toString();
    }

    private String concatenarInstHexaDosOperandos(byte primerByte, int operandoB, int cantBytesOpB, int operandoA, int cantBytesOpA) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%02X", primerByte & 0xFF));
        
        
        for (int i = 0; i < cantBytesOpB; i++) {
            sb.append(String.format(" %02X", (operandoB >> (i * 8)) & 0xFF));
        }
        
        
        for (int i = 0; i < cantBytesOpA; i++) {
            sb.append(String.format(" %02X", (operandoA >> (i * 8)) & 0xFF));
        }
        
        return sb.toString();
    }

    public void disassembleAll() {
        
        DescriptorSegmento segmentoCS = tabla.getSegmento("CS");
        if (segmentoCS == null) return;

        int baseCS = segmentoCS.getBase();
        int limiteCS = baseCS + segmentoCS.getTamanio();
        int ip = baseCS;

        while (ip < limiteCS) {
            registros.setIP(ip);
            byte primerByte = memoria.leerPrimerByte(ip);
            byte codOperacion = (byte) (primerByte & 0x1F);
            int bytesLeidos = decodificarInstruccion(primerByte);

            if (bytesLeidos <= 0) {
                ip++; 
                continue;
            }

            ip += bytesLeidos;

            if (codOperacion == 0x0F) { 
                break;
            }
        }
    }
} 