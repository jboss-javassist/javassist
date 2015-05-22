/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */
package javassist.bytecode.analysis;


/**
 * Represents the stack frame and local variable table at a particular point in time.
 *
 * @author Jason T. Greene
 */
public class Frame {
    private Type[] locals;
    private Type[] stack;
    private int top;
    private boolean jsrMerged;
    private boolean retMerged;

    /**
     * Create a new frame with the specified local variable table size, and max stack size
     *
     * @param locals the number of local variable table entries
     * @param stack the maximum stack size
     */
    public Frame(int locals, int stack) {
        this.locals = new Type[locals];
        this.stack = new Type[stack];
    }

    /**
     * Returns the local varaible table entry at index.
     *
     * @param index the position in the table
     * @return the type if one exists, or null if the position is empty
     */
    public Type getLocal(int index) {
        return locals[index];
    }

    /**
     * Sets the local variable table entry at index to a type.
     *
     * @param index the position in the table
     * @param type the type to set at the position
     */
    public void setLocal(int index, Type type) {
        locals[index] = type;
    }


    /**
     * Returns the type on the stack at the specified index.
     *
     * @param index the position on the stack
     * @return the type of the stack position
     */
    public Type getStack(int index) {
        return stack[index];
    }

    /**
     * Sets the type of the stack position
     *
     * @param index the position on the stack
     * @param type the type to set
     */
    public void setStack(int index, Type type) {
        stack[index] = type;
    }

    /**
     * Empties the stack
     */
    public void clearStack() {
        top = 0;
    }

    /**
     * Gets the index of the type sitting at the top of the stack.
     * This is not to be confused with a length operation which
     * would return the number of elements, not the position of
     * the last element.
     *
     * @return the position of the element at the top of the stack
     */
    public int getTopIndex() {
        return top - 1;
    }

    /**
     * Returns the number of local variable table entries, specified
     * at construction.
     *
     * @return the number of local variable table entries
     */
    public int localsLength() {
        return locals.length;
    }

    /**
     * Gets the top of the stack without altering it
     *
     * @return the top of the stack
     */
    public Type peek() {
        if (top < 1)
            throw new IndexOutOfBoundsException("Stack is empty");

        return stack[top - 1];
    }

    /**
     * Alters the stack to contain one less element and return it.
     *
     * @return the element popped from the stack
     */
    public Type pop() {
        if (top < 1)
            throw new IndexOutOfBoundsException("Stack is empty");
        return stack[--top];
    }

    /**
     * Alters the stack by placing the passed type on the top
     *
     * @param type the type to add to the top
     */
    public void push(Type type) {
        stack[top++] = type;
    }


    /**
     * Makes a shallow copy of this frame, i.e. the type instances will
     * remain the same.
     *
     * @return the shallow copy
     */
    public Frame copy() {
        Frame frame = new Frame(locals.length, stack.length);
        System.arraycopy(locals, 0, frame.locals, 0, locals.length);
        System.arraycopy(stack, 0, frame.stack, 0, stack.length);
        frame.top = top;
        return frame;
    }

    /**
     * Makes a shallow copy of the stack portion of this frame. The local
     * variable table size will be copied, but its contents will be empty.
     *
     * @return the shallow copy of the stack
     */
    public Frame copyStack() {
        Frame frame = new Frame(locals.length, stack.length);
        System.arraycopy(stack, 0, frame.stack, 0, stack.length);
        frame.top = top;
        return frame;
    }

    /**
     * Merges all types on the stack of this frame instance with that of the specified frame.
     * The local variable table is left untouched.
     *
     * @param frame the frame to merge the stack from
     * @return true if any changes where made
     */
    public boolean mergeStack(Frame frame) {
        boolean changed = false;
        if (top != frame.top)
            throw new RuntimeException("Operand stacks could not be merged, they are different sizes!");

        for (int i = 0; i < top; i++) {
            if (stack[i] != null) {
                Type prev = stack[i];
                Type merged = prev.merge(frame.stack[i]);
                if (merged == Type.BOGUS)
                    throw new RuntimeException("Operand stacks could not be merged due to differing primitive types: pos = " + i);

                stack[i] = merged;
                // always replace the instance in case a multi-interface type changes to a normal Type
                if ((! merged.equals(prev)) || merged.popChanged()) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    /**
     * Merges all types on the stack and local variable table of this frame with that of the specified
     * type.
     *
     * @param frame the frame to merge with
     * @return true if any changes to this frame where made by this merge
     */
    public boolean merge(Frame frame) {
        boolean changed = false;

        // Local variable table
        for (int i = 0; i < locals.length; i++) {
            if (locals[i] != null) {
                Type prev = locals[i];
                Type merged = prev.merge(frame.locals[i]);
                // always replace the instance in case a multi-interface type changes to a normal Type
                locals[i] = merged;
                if (! merged.equals(prev) || merged.popChanged()) {
                    changed = true;
                }
            } else if (frame.locals[i] != null) {
                locals[i] = frame.locals[i];
                changed = true;
            }
        }

        changed |= mergeStack(frame);
        return changed;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("locals = [");
        for (int i = 0; i < locals.length; i++) {
            buffer.append(locals[i] == null ? "empty" : locals[i].toString());
            if (i < locals.length - 1)
                buffer.append(", ");
        }
        buffer.append("] stack = [");
        for (int i = 0; i < top; i++) {
            buffer.append(stack[i]);
            if (i < top - 1)
                buffer.append(", ");
        }
        buffer.append("]");

        return buffer.toString();
    }

    /**
     * Whether or not state from the source JSR instruction has been merged
     *
     * @return true if JSR state has been merged
     */
    boolean isJsrMerged() {
        return jsrMerged;
    }

    /**
     * Sets whether of not the state from the source JSR instruction has been merged
     *
     * @param jsrMerged true if merged, otherwise false
     */
    void setJsrMerged(boolean jsrMerged) {
        this.jsrMerged = jsrMerged;
    }

    /**
     * Whether or not state from the RET instruction, of the subroutine that was jumped
     * to has been merged.
     *
     * @return true if RET state has been merged
     */
    boolean isRetMerged() {
        return retMerged;
    }

    /**
     * Sets whether or not state from the RET instruction, of the subroutine that was jumped
     * to has been merged.
     *
     * @param retMerged true if RET state has been merged
     */
    void setRetMerged(boolean retMerged) {
        this.retMerged = retMerged;
    }
}
