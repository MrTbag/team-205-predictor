package hardwar.branch.prediction.judged.GAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;
import java.util.BitSet;
import java.lang.Math;

public class GAg implements BranchPredictor {
    private final ShiftRegister BHR; // branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final ShiftRegister SC; // saturated counter register

    public GAg() {
        this(4, 2);
    }

    /**
     * Creates a new GAg predictor with the given BHR register size and initializes the BHR and PHT.
     *
     * @param BHRSize the size of the BHR register
     * @param SCSize  the size of the register which hold the saturating counter value and the cache block size
     */
    public GAg(int BHRSize, int SCSize) {
        // TODO : complete the constructor
        // Initialize the BHR register with the given size and no default value
       
        this.BHR = new SIPORegister(null, BHRSize, getDefaultBlock());

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        this.PHT = new PageHistoryTable((int) Math.pow(2, BHRSize), SCSize);
        for (int i = 0; i < (int) Math.pow(2, BHRSize); i ++){
            String st = Integer.toBinaryString(i);
            Bit bits[];
            bits = new Bit[st.length()];
            for (int j = 0; j < st.length(); j ++){
                if (st.charAt(j) == '0')
                    bits[j] = Bit.ZERO;
                else
                    bits[j] = Bit.ONE;
            }
            Bit[] values = {Bit.ZERO, Bit.ZERO};
            this.PHT.put(bits, values);
        }

        // Initialize the SC register
        SC = new SIPORegister(null, SCSize, getDefaultBlock());

    }

    /**
     * Predicts the result of a branch instruction based on the global branch history
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        Bit[] bits = this.BHR.read();
        this.SC.load(this.PHT.get(bits));
        return BranchResult.of(this.SC.read()[0].getValue());
    }

    /**
     * Updates the values in the cache based on the actual branch result
     *
     * @param instruction the branch instruction
     * @param actual      the actual result of the branch condition
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        Bit[] bits = this.BHR.read();
        this.SC.load(this.PHT.get(bits));
        if (BranchResult.isTaken(actual)){
            this.SC.load(CombinationalLogic.count(SC.read(), true, CountMode.SATURATING));
            this.PHT.put(bits, this.SC.read());
        }
        else{
            this.SC.load(CombinationalLogic.count(SC.read(), false, CountMode.SATURATING));
            this.PHT.put(bits, this.SC.read());
        }
        this.BHR.insert(this.SC.read()[0]);
    }


    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "GAg predictor snapshot: \n" + BHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
