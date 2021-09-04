package com.lewisk.emulation;

import static com.lewisk.emulation.CPU6502.Flags;

// Returns binaries to be executed on a CPU6502 for a given Instruction.
public abstract class Opcode6502
{
    // Interface used to execute instruction on CPU.
    protected interface ExecutionInterface
    {
        /*
         *  Takes CPU source to execute instruction on.
         *  @returns cycles
         */
        int execute(CPU6502 cpu);
    }

    protected static int doBranch(CPU6502 cpu, boolean condition)
    {
        byte relAddr = (byte) cpu.nextBytePC();
        if(condition)
        {
            cpu.registers.PC += relAddr;
            return 3;
        }
        return 2;
    }
    protected static void doROL(CPU6502 cpu, short addr)
    {
        byte result = (byte) (cpu.readRAM(addr) << 1);
        boolean setRoll = cpu.getFlag(Flags.Carry);
        if(setRoll) result |= 1; else result &= ~1;
        cpu.setFlag(Flags.Carry, (cpu.readRAM(addr) & (1 << 7)) != 0);
        cpu.writeRAM(addr, result);
        cpu.setFlagsResult(result);
    }
    protected static void doROR(CPU6502 cpu, short addr)
    {
        byte result = (byte) (cpu.readRAM(addr) >> 1);
        boolean setRoll = cpu.getFlag(Flags.Carry);
        if(setRoll) result |= (1 << 7); else result &= ~(1 << 7);
        cpu.setFlag(Flags.Carry, (cpu.readRAM(addr) & 1) != 0);
        cpu.writeRAM(addr, result);
        cpu.setFlagsResult(result);
    }
    protected static void doLSR(CPU6502 cpu, short addr)
    {
        byte result = (byte) (cpu.readRAM(addr) >> 1);
        cpu.setFlag(Flags.Carry, (cpu.readRAM(addr) & 1) != 0);
        cpu.writeRAM(addr, result);
        cpu.setFlagsResult(result);
    }
    protected static void doASL(CPU6502 cpu, short addr)
    {
        byte result = (byte) (cpu.readRAM(addr) << 1);
        cpu.setFlag(Flags.Carry, (cpu.readRAM(addr) & (1 << 7)) != 0);
        cpu.writeRAM(addr, result);
        cpu.setFlagsResult(result);
    }
    protected static void doADC(CPU6502 cpu, int value)
    {
        byte num = (byte) (value + (cpu.getFlag(Flags.Carry) ? 1 : 0));
        cpu.setFlagsADC(cpu.registers.A,num);
        cpu.registers.A += num;
    }
    protected static void doSBC(CPU6502 cpu, int value)
    {
        byte num = (byte) (value + ((!cpu.getFlag(Flags.Carry)) ? 1 : 0));
        cpu.setFlagsSBC(cpu.registers.A,num);
        cpu.registers.A -= num;
    }

    public static ExecutionInterface getInstruction(byte opcode)
    {
        return switch (opcode) {
            // LDA - Immediate
            case (byte) 0xA9 -> (cpu) -> {
                cpu.setRegA(cpu.nextBytePC());
                return 2;
            };
            // LDA - Zero Page
            case (byte) 0xA5 -> (cpu) -> {
                cpu.setRegA(cpu.readZeroPage());
                return 3;
            };
            // LDA - Zero Page, X
            case (byte) 0xB5 -> (cpu) -> {
                cpu.setRegA(cpu.readZeroPage(cpu.registers.X));
                return 4;
            };
            // LDA - Absolute
            case (byte) 0xAD -> (cpu) -> {
                cpu.setRegA(cpu.readAbsolute());
                return 4;
            };
            // LDA - Absolute, X
            case (byte) 0xBD -> (cpu) -> {
                cpu.setRegA(cpu.readAbsolute(cpu.registers.X));
                return 4;
            };
            // LDA - Absolute, Y
            case (byte) 0xB9 -> (cpu) -> {
                cpu.setRegA(cpu.readAbsolute(cpu.registers.Y));
                return 4;
            };
            // LDA - Indirect, X
            case (byte) 0xA1 -> (cpu) -> {
                cpu.setRegA(cpu.readRAM(cpu.indirectX()));
                return 6;
            };
            // LDA - Indirect, Y
            case (byte) 0xB1 -> (cpu) -> {
                cpu.setRegA(cpu.readRAM(cpu.indirectY()));
                return 5;
            };

            // LDX - Immediate
            case (byte) 0xA2 -> (cpu) -> {
                cpu.setRegX(cpu.nextBytePC());
                return 2;
            };
            // LDX - Zero Page
            case (byte) 0xA6 -> (cpu) -> {
                cpu.setRegX(cpu.readZeroPage());
                return 3;
            };
            // LDX - Zero Page, Y
            case (byte) 0xB6 -> (cpu) -> {
                cpu.setRegX(cpu.readZeroPage(cpu.registers.Y));
                return 4;
            };
            // LDX - Absolute
            case (byte) 0xAE -> (cpu) -> {
                cpu.setRegX(cpu.readAbsolute());
                return 4;
            };
            // LDX - Absolute, Y
            case (byte) 0xBE -> (cpu) -> {
                cpu.setRegX(cpu.readAbsolute(cpu.registers.Y));
                return 4;
            };

            // LDY - Immediate
            case (byte) 0xA0 -> (cpu) -> {
                cpu.setRegY(cpu.nextBytePC());
                return 2;
            };
            // LDY - Zero Page
            case (byte) 0xA4 -> (cpu) -> {
                cpu.setRegY(cpu.readZeroPage());
                return 3;
            };
            // LDY - Zero Page, X
            case (byte) 0xB4 -> (cpu) -> {
                cpu.setRegY(cpu.readZeroPage(cpu.registers.X));
                return 4;
            };
            // LDY - Absolute
            case (byte) 0xAC -> (cpu) -> {
                cpu.setRegY(cpu.readAbsolute());
                return 4;
            };
            // LDY - Absolute, X
            case (byte) 0xBC -> (cpu) -> {
                cpu.setRegY(cpu.readAbsolute(cpu.registers.X));
                return 4;
            };

            // STA - Zero Page
            case (byte) 0x85 -> (cpu) -> {
                cpu.writeRAM(cpu.zeroPage(), cpu.registers.A);
                return 3;
            };
            // STA - Zero Page, X
            case (byte) 0x95 -> (cpu) -> {
                cpu.writeRAM(cpu.zeroPage(cpu.registers.X), cpu.registers.A);
                return 4;
            };
            // STA - Absolute
            case (byte) 0x8D -> (cpu) -> {
                cpu.writeRAM(cpu.absolute(), cpu.registers.A);
                return 4;
            };
            // STA - Absolute, X
            case (byte) 0x9D -> (cpu) -> {
                cpu.writeRAM(cpu.absolute(cpu.registers.X), cpu.registers.A);
                return 5;
            };
            // STA - Absolute, Y
            case (byte) 0x99 -> (cpu) -> {
                cpu.writeRAM(cpu.absolute(cpu.registers.Y), cpu.registers.A);
                return 5;
            };
            // STA - Indirect, X
            case (byte) 0x81 -> (cpu) -> {
                cpu.writeRAM(cpu.indirectX(), cpu.registers.A);
                return 6;
            };
            // STA - Indirect, X
            case (byte) 0x91 -> (cpu) -> {
                cpu.writeRAM(cpu.indirectY(), cpu.registers.A);
                return 6;
            };

            // STX - Zero Page
            case (byte) 0x86 -> (cpu) -> {
                cpu.writeRAM(cpu.zeroPage(), cpu.registers.X);
                return 3;
            };
            // STX - Zero Page, Y
            case (byte) 0x96 -> (cpu) -> {
                cpu.writeRAM(cpu.zeroPage(cpu.registers.Y), cpu.registers.X);
                return 4;
            };
            // STX - Absolute
            case (byte) 0x8E -> (cpu) -> {
                cpu.writeRAM(cpu.absolute(), cpu.registers.X);
                return 4;
            };

            // STY - Zero Page
            case (byte) 0x84 -> (cpu) -> {
                cpu.writeRAM(cpu.zeroPage(), cpu.registers.Y);
                return 3;
            };
            // STY - Zero Page, X
            case (byte) 0x94 -> (cpu) -> {
                cpu.writeRAM(cpu.zeroPage(cpu.registers.X), cpu.registers.Y);
                return 4;
            };
            // STY - Absolute
            case (byte) 0x8C -> (cpu) -> {
                cpu.writeRAM(cpu.absolute(), cpu.registers.Y);
                return 4;
            };

            // TAX - Implied
            case (byte) 0xAA -> (cpu) -> {
                cpu.setRegX(cpu.registers.A);
                return 2;
            };
            // TAY - Implied
            case (byte) 0xA8 -> (cpu) -> {
                cpu.setRegY(cpu.registers.A);
                return 2;
            };
            // TXA - Implied
            case (byte) 0x8A -> (cpu) -> {
                cpu.setRegA(cpu.registers.X);
                return 2;
            };
            // TYA - Implied
            case (byte) 0x98 -> (cpu) -> {
                cpu.setRegA(cpu.registers.Y);
                return 2;
            };
            // TSX - Implied
            case (byte) 0xBA -> (cpu) -> {
                cpu.setRegY(cpu.registers.SP);
                return 2;
            };
            // TXS - Implied
            case (byte) 0x9A -> (cpu) -> {
                cpu.registers.SP = cpu.registers.X;
                return 2;
            };

// -- Arithmetic -- \\
            // ADC - Immediate
            case (byte) 0x69 -> (cpu) -> {
                doADC(cpu, cpu.nextBytePC());
                return 2;
            };
            // ADC - Zero Page
            case (byte) 0x65 -> (cpu) -> {
                doADC(cpu, cpu.zeroPage());
                return 3;
            };
            // ADC - Zero Page, X
            case (byte) 0x75 -> (cpu) -> {
                doADC(cpu, cpu.zeroPage(cpu.registers.X));
                return 4;
            };
            // ADC - Absolute
            case (byte) 0x6D -> (cpu) -> {
                doADC(cpu, cpu.absolute());
                return 4;
            };
            // ADC - Absolute, X
            case (byte) 0x7D -> (cpu) -> {
                doADC(cpu, cpu.absolute(cpu.registers.X));
                return 4;
            };
            // ADC - Absolute, Y
            case (byte) 0x79 -> (cpu) -> {
                doADC(cpu, cpu.absolute(cpu.registers.Y));
                return 4;
            };
            // ADC - Indirect, X
            case (byte) 0x61 -> (cpu) -> {
                doADC(cpu,cpu.readIndirectX());
                return 6;
            };
            // ADC - Indirect, Y
            case (byte) 0x71 -> (cpu) -> {
                doADC(cpu,cpu.readIndirectY());
                return 5;
            };

            // SBC - Immediate
            case (byte) 0xE9 -> (cpu) -> {
                doSBC(cpu, cpu.nextBytePC());
                return 2;
            };
            // SBC - Zero Page
            case (byte) 0xE5 -> (cpu) -> {
                doSBC(cpu, cpu.zeroPage());
                return 3;
            };
            // SBC - Zero Page, X
            case (byte) 0xF5 -> (cpu) -> {
                doSBC(cpu, cpu.zeroPage(cpu.registers.X));
                return 4;
            };
            // SBC - Absolute
            case (byte) 0xFD -> (cpu) -> {
                doSBC(cpu, cpu.absolute());
                return 4;
            };
            // SBC - Absolute, X
            case (byte) 0xED -> (cpu) -> {
                doSBC(cpu, cpu.absolute(cpu.registers.X));
                return 4;
            };
            // SBC - Absolute, Y
            case (byte) 0xF9 -> (cpu) -> {
                doSBC(cpu, cpu.absolute(cpu.registers.Y));
                return 4;
            };
            // SBC - Indirect, X
            case (byte) 0xE1 -> (cpu) -> {
                doSBC(cpu,cpu.readIndirectX());
                return 6;
            };
            // SBC - Indirect, Y
            case (byte) 0xF1 -> (cpu) -> {
                doSBC(cpu,cpu.readIndirectY());
                return 5;
            };

            // INC - Zero Page
            case (byte) 0xE6 -> (cpu) -> {
                short addr = cpu.zeroPage();
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)+1);
                return 5;
            };
            // INC - Zero Page, X
            case (byte) 0xF6 -> (cpu) -> {
                short addr = cpu.zeroPage(cpu.registers.X);
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)+1);
                return 6;
            };
            // INC - Absolute
            case (byte) 0xEE -> (cpu) -> {
                short addr = cpu.absolute();
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)+1);
                return 6;
            };
            // INC - Absolute, X
            case (byte) 0xFE -> (cpu) -> {
                short addr = cpu.absolute(cpu.registers.X);
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)+1);
                return 7;
            };

            // INX - Implied
            case (byte) 0xE8 -> (cpu) -> {
                cpu.setRegX((byte) (cpu.registers.X+1));
                return 2;
            };

            // INY - Implied
            case (byte) 0xC8 -> (cpu) -> {
                cpu.setRegY((byte) (cpu.registers.Y+1));
                return 2;
            };

            // DEC - Zero Page
            case (byte) 0xC6 -> (cpu) -> {
                short addr = cpu.zeroPage();
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)-1);
                return 5;
            };
            // DEC - Zero Page, X
            case (byte) 0xD6 -> (cpu) -> {
                short addr = cpu.zeroPage(cpu.registers.X);
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)-1);
                return 6;
            };
            // DEC - Absolute
            case (byte) 0xCE -> (cpu) -> {
                short addr = cpu.absolute();
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)-1);
                return 6;
            };
            // DEC - Absolute, X
            case (byte) 0xDE -> (cpu) -> {
                short addr = cpu.absolute(cpu.registers.X);
                cpu.writeRAM_WithFlags(addr, cpu.readRAM(addr)-1);
                return 7;
            };

            // DEX - Implied
            case (byte) 0xCA -> (cpu) -> {
                cpu.setRegX((byte) (cpu.registers.X-1));
                return 2;
            };

            // DEY - Implied
            case (byte) 0x88 -> (cpu) -> {
                cpu.setRegY((byte) (cpu.registers.Y-1));
                return 2;
            };

// -- Bitwise Arithmetic -- \\
            // ORA - Immediate
            case (byte) 0x09 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.nextBytePC()));
                return 2;
            };
            // ORA - Zero Page
            case (byte) 0x05 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.readZeroPage()));
                return 3;
            };
            // ORA - Zero Page, X
            case (byte) 0x15 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.readZeroPage(cpu.registers.X)));
                return 4;
            };
            // ORA - Absolute
            case (byte) 0x0D -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.readAbsolute()));
                return 4;
            };
            // ORA - Absolute, X
            case (byte) 0x1D -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.readAbsolute(cpu.registers.X)));
                return 4;
            };
            // ORA - Absolute, Y
            case (byte) 0x19 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.readAbsolute(cpu.registers.Y)));
                return 4;
            };
            // ORA - Indirect, X
            case (byte) 0x01 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.readIndirectX()));
                return 6;
            };
            // ORA - Indirect, Y
            case (byte) 0x11 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A | cpu.readIndirectY()));
                return 5;
            };

            // EOR - Immediate
            case (byte) 0x49 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.nextBytePC()));
                return 2;
            };
            // EOR - Zero Page
            case (byte) 0x45 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.readZeroPage()));
                return 3;
            };
            // EOR - Zero Page, X
            case (byte) 0x55 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.readZeroPage(cpu.registers.X)));
                return 4;
            };
            // EOR - Absolute
            case (byte) 0x4D -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.readAbsolute()));
                return 4;
            };
            // EOR - Absolute, X
            case (byte) 0x5D -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.readAbsolute(cpu.registers.X)));
                return 4;
            };
            // EOR - Absolute, Y
            case (byte) 0x59 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.readAbsolute(cpu.registers.Y)));
                return 4;
            };
            // EOR - Indirect, X
            case (byte) 0x41 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.readIndirectX()));
                return 6;
            };
            // EOR - Indirect, Y
            case (byte) 0x51 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A ^ cpu.readIndirectY()));
                return 5;
            };

            // AND - Immediate
            case (byte) 0x29 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.nextBytePC()));
                return 2;
            };
            // AND - Zero Page
            case (byte) 0x25 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.readZeroPage()));
                return 3;
            };
            // AND - Zero Page, X
            case (byte) 0x35 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.readZeroPage(cpu.registers.X)));
                return 4;
            };
            // AND - Absolute
            case (byte) 0x2D -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.readAbsolute()));
                return 4;
            };
            // AND - Absolute, X
            case (byte) 0x3D -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.readAbsolute(cpu.registers.X)));
                return 4;
            };
            // AND - Absolute, Y
            case (byte) 0x39 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.readAbsolute(cpu.registers.Y)));
                return 4;
            };
            // AND - Indirect, X
            case (byte) 0x21 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.readIndirectX()));
                return 6;
            };
            // AND - Indirect, Y
            case (byte) 0x31 -> (cpu) -> {
                cpu.setRegA((byte) (cpu.registers.A & cpu.readIndirectY()));
                return 5;
            };

            // LSR - Accumulator
            case (byte) 0x4A -> (cpu) -> {
                cpu.setFlag(Flags.Carry, (cpu.registers.A & 1) != 0);
                cpu.setRegA((byte) (cpu.registers.A >> 1));
                return 2;
            };
            // LSR - Zero Page
            case (byte) 0x46 -> (cpu) -> {
                doLSR(cpu, cpu.zeroPage());
                return 5;
            };
            // LSR - Zero Page, X
            case (byte) 0x56 -> (cpu) -> {
                doLSR(cpu, cpu.zeroPage(cpu.registers.X));
                return 6;
            };
            // LSR - Absolute
            case (byte) 0x4E -> (cpu) -> {
                doLSR(cpu, cpu.absolute());
                return 6;
            };
            // LSR - Absolute, X
            case (byte) 0x5E -> (cpu) -> {
                doLSR(cpu, cpu.absolute(cpu.registers.X));
                return 7;
            };

            // ASL - Accumulator
            case (byte) 0x0A -> (cpu) -> {
                cpu.setFlag(Flags.Carry, (cpu.registers.A & (1 << 7)) != 0);
                cpu.setRegA((byte) (cpu.registers.A << 1));
                return 2;
            };
            // ASL - Zero Page
            case (byte) 0x06 -> (cpu) -> {
                doASL(cpu, cpu.zeroPage());
                return 5;
            };
            // ASL - Zero Page, X
            case (byte) 0x16 -> (cpu) -> {
                doASL(cpu, cpu.zeroPage(cpu.registers.X));
                return 6;
            };
            // ASL - Absolute
            case (byte) 0x0E -> (cpu) -> {
                doASL(cpu, cpu.absolute());
                return 6;
            };
            // ASL - Absolute, X
            case (byte) 0x1E -> (cpu) -> {
                doASL(cpu, cpu.absolute(cpu.registers.X));
                return 7;
            };

            // ROL - Accumulator
            case (byte) 0x2A -> (cpu) -> {
                byte result = (byte) (cpu.registers.A << 1);
                boolean setRoll = cpu.getFlag(Flags.Carry);
                if(setRoll) result |= 1; else result &= ~1;
                cpu.setFlag(Flags.Carry, (cpu.registers.A & (1 << 7)) != 0);
                cpu.setRegA(result);
                return 2;
            };
            // ROL - Zero Page
            case (byte) 0x26 -> (cpu) -> {
                doROL(cpu, cpu.zeroPage());
                return 5;
            };
            // ROL - Zero Page, X
            case (byte) 0x36 -> (cpu) -> {
                doROL(cpu, cpu.zeroPage(cpu.registers.X));
                return 6;
            };
            // ROL - Absolute
            case (byte) 0x2E -> (cpu) -> {
                doROL(cpu, cpu.absolute());
                return 6;
            };
            // ROL - Absolute, X
            case (byte) 0x3E -> (cpu) -> {
                doROL(cpu, cpu.absolute(cpu.registers.X));
                return 7;
            };

            // ROR - Accumulator
            case (byte) 0x6A -> (cpu) -> {
                byte result = (byte) (cpu.registers.A >> 1);
                boolean setRoll = cpu.getFlag(Flags.Carry);
                if(setRoll) result |= (1 << 7); else result &= ~(1 << 7);
                cpu.setFlag(Flags.Carry, (cpu.registers.A & 1) != 0);
                cpu.setRegA(result);
                return 2;
            };
            // ROR - Zero Page
            case (byte) 0x66 -> (cpu) -> {
                doROR(cpu, cpu.zeroPage());
                return 5;
            };
            // ROR - Zero Page, X
            case (byte) 0x76 -> (cpu) -> {
                doROR(cpu, cpu.zeroPage(cpu.registers.X));
                return 6;
            };
            // ROR - Absolute
            case (byte) 0x6E -> (cpu) -> {
                doROR(cpu, cpu.absolute());
                return 6;
            };
            // ROR - Absolute, X
            case (byte) 0x7E -> (cpu) -> {
                doROR(cpu, cpu.absolute(cpu.registers.X));
                return 7;
            };

// -- Stack -- \\
            // PHA - Implied
            case (byte) 0x48 -> (cpu) -> {
                cpu.stackPush(cpu.registers.A);
                return 3;
            };
            // PLA - Implied
            case (byte) 0x68 -> (cpu) -> {
                cpu.setRegA(cpu.stackPop());
                return 4;
            };

            // PHP - Implied
            case (byte) 0x08 -> (cpu) -> {
                cpu.stackPush(cpu.cpuFlags);
                return 3;
            };
            // PLP - Implied
            case (byte) 0x28 -> (cpu) -> {
                cpu.cpuFlags = cpu.stackPop();
                return 4;
            };

// -- Compare -- \\
            // CMP - Immediate
            case (byte) 0xC9 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.nextBytePC());
                return 2;
            };
            // CMP - Zero Page
            case (byte) 0xC5 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.readZeroPage());
                return 3;
            };
            // CMP - Zero Page, X
            case (byte) 0xD5 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.readZeroPage(cpu.registers.X));
                return 4;
            };
            // CMP - Absolute
            case (byte) 0xCD -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.readAbsolute());
                return 4;
            };
            // CMP - Absolute, X
            case (byte) 0xDD -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.readAbsolute(cpu.registers.X));
                return 4;
            };
            // CMP - Absolute, Y
            case (byte) 0xD9 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.readAbsolute(cpu.registers.Y));
                return 4;
            };
            // CMP - Indirect, X
            case (byte) 0xC1 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.readIndirectX());
                return 6;
            };
            // CMP - Indirect, Y
            case (byte) 0xD1 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.A, cpu.readIndirectY());
                return 5;
            };

            // CPX - Immediate
            case (byte) 0xE0 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.X, cpu.nextBytePC());
                return 2;
            };
            // CPX - Zero Page
            case (byte) 0xE4 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.X, cpu.readZeroPage());
                return 3;
            };
            // CPX - Absolute
            case (byte) 0xEC -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.X, cpu.readAbsolute());
                return 4;
            };

            // CPY - Immediate
            case (byte) 0xC0 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.Y, cpu.nextBytePC());
                return 2;
            };
            // CPY - Zero Page
            case (byte) 0xC4 -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.Y, cpu.readZeroPage((byte) 0));
                return 3;
            };
            // CPY - Absolute
            case (byte) 0xCC -> (cpu) -> {
                cpu.setFlagsCMP(cpu.registers.Y, cpu.readAbsolute());
                return 4;
            };

            // BIT - Zero Page
            case (byte) 0x24 -> (cpu) -> {
                short value = cpu.readZeroPage();
                cpu.setFlag(Flags.Zero, (cpu.registers.A & value) == 0);
                cpu.setFlag(Flags.Overflow, (value & (1 << 6)) != 0);
                cpu.setFlag(Flags.Negative, (value & (1 << 7)) != 0);
                return 3;
            };

            // BIT - Absolute
            case (byte) 0x2C -> (cpu) -> {
                short value = cpu.readAbsolute();
                cpu.setFlag(Flags.Zero, (cpu.registers.A & value) == 0);
                cpu.setFlag(Flags.Overflow, (value & (1 << 6)) != 0);
                cpu.setFlag(Flags.Negative, (value & (1 << 7)) != 0);
                return 4;
            };

// -- Branching -- \\
            // JMP - Absolute
            case (byte) 0x4C -> (cpu) -> {
                cpu.registers.PC = (short) (cpu.absolute()-1);
                return 3;
            };
            // JMP - Indirect
            case (byte) 0x6C -> (cpu) -> {
                short zp = cpu.immediateAddress();
                cpu.registers.PC = cpu.bytesToShort(cpu.readRAM(zp), cpu.readRAM(++zp));
                return 5;
            };

            // JSR - Absolute
            case (byte) 0x20 -> (cpu) -> {
                /*
                 * A 2 byte offset is needed due to how the program counter
                 * is incremented, it's currently sitting at the end of the
                 * previous instruction.
                 */
                cpu.pushPC(2);

                // Subtract 1 because 1 will be added to the PC after this instruction runs.
                cpu.registers.PC = (short) (cpu.nextBytesPC() - 1);
                return 6;
            };

            // RTS - Implied
            case (byte) 0x60 -> (cpu) -> {
                cpu.popPC();
                return 6;
            };

            // BEQ - Relative
            case (byte) 0xF0 -> (cpu) -> doBranch(cpu,  cpu.getFlag(Flags.Zero));
            // BNE - Relative
            case (byte) 0xD0 -> (cpu) -> doBranch(cpu, !cpu.getFlag(Flags.Zero));
            // BMI - Relative
            case (byte) 0x30 -> (cpu) -> doBranch(cpu,  cpu.getFlag(Flags.Negative));
            // BPL - Relative
            case (byte) 0x10 -> (cpu) -> doBranch(cpu, !cpu.getFlag(Flags.Negative));
            // BCS - Relative
            case (byte) 0xB0 -> (cpu) -> doBranch(cpu,  cpu.getFlag(Flags.Carry));
            // BCC - Relative
            case (byte) 0x90 -> (cpu) -> doBranch(cpu, !cpu.getFlag(Flags.Carry));
            // BVS - Relative
            case (byte) 0x70 -> (cpu) -> doBranch(cpu,  cpu.getFlag(Flags.Overflow));
            // BVC - Relative
            case (byte) 0x50 -> (cpu) -> doBranch(cpu, !cpu.getFlag(Flags.Overflow));

// -- Flags -- \\
            // SEC - Implied
            case (byte) 0x38 -> (cpu) -> {
                cpu.setFlag(Flags.Carry,true);
                return 2;
            };
            // SED - Implied
            case (byte) 0xF8 -> (cpu) -> {
                cpu.setFlag(Flags.DecimalMode,true);
                return 2;
            };
            // SEI - Implied
            case (byte) 0x78 -> (cpu) -> {
                cpu.setFlag(Flags.InterruptDisable,true);
                return 2;
            };

            // CLC - Implied
            case (byte) 0x18 -> (cpu) -> {
                cpu.setFlag(Flags.Carry,false);
                return 2;
            };
            // CLD - Implied
            case (byte) 0xD8 -> (cpu) -> {
                cpu.setFlag(Flags.DecimalMode,false);
                return 2;
            };
            // CLI - Implied
            case (byte) 0x58 -> (cpu) -> {
                cpu.setFlag(Flags.InterruptDisable,false);
                return 2;
            };
            // CLV - Implied
            case (byte) 0xB8 -> (cpu) -> {
                cpu.setFlag(Flags.Overflow,false);
                return 2;
            };

// -- Misc -- \\
            // RTI - Implied
            case (byte) 0x40 -> (cpu) -> {
                cpu.cpuFlags = cpu.stackPop();
                cpu.popPC();
                return 6;
            };

            // BRK - Implied
            case (byte) 0x00 -> (cpu) -> {
                cpu.pushPC(0);
                cpu.stackPush(cpu.cpuFlags);
                cpu.setFlag(Flags.Break, true);
                cpu.halt = true;
                return 7;
            };

            // NOP - Implied
            case (byte) 0xEA -> (cpu) -> 2;

            // Halt on unknown instruction.
            default -> (cpu) -> {
                System.out.format("Invalid opcode encountered: 0x%02x\n", opcode);
                cpu.halt = true;
                return 1;
            };
        };
    }
}
