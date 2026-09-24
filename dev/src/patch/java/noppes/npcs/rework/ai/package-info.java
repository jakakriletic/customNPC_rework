/**
 * M3.6 — razvrscanje AI taskov NPC-ja (boj proti gibanju).
 *
 * <p>{@link noppes.npcs.rework.ai.AttackPriority} odloci, ali ima napad prednost pred
 * gibalnim taskom, ki mu sledi ({@code EntityAIWander}, {@code EntityAIMovingPath}).
 * Odlocitev je cista funkcija brez Minecrafta; test jo preveri na pravem vanilla
 * {@code EntityAITasks}.
 *
 * <p>Privzeto je nacin {@link noppes.npcs.rework.ai.AttackPriority#ORIGINAL} (D-007):
 * prioritete taskov so enake originalu.
 */
package noppes.npcs.rework.ai;
