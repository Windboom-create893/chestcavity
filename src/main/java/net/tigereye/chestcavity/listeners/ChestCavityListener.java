package net.tigereye.chestcavity.listeners;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.items.ChestCavityOrgan;
import net.tigereye.chestcavity.items.VanillaOrgans;


public class ChestCavityListener implements InventoryChangedListener {
	
	PlayerEntity player;

	private int spleenTimer = 0;
	private float lungLeftover;

	private Map<Identifier,Float> organScores = new HashMap<>();

	
	public ChestCavityListener(PlayerEntity player)
	{
		this.player = player;
	}

	@Override
	public void onInventoryChanged(Inventory sender)
	{
		EvaluateChestCavity(sender);
	}

	//calculates the 'organ scores' of the player's chest cavity
	//returns true if any of the scores changed
	public boolean EvaluateChestCavity(Inventory inv)
	{
		Map<Identifier,Float> oldScores = new HashMap<>(organScores);
		organScores.clear();

		for (int i = 0; i < inv.size(); i++)
		{
			ItemStack slot = inv.getStack(i);
			if (slot != null)
			{
				Item slotitem = slot.getItem();
				if(slotitem instanceof ChestCavityOrgan){
					((ChestCavityOrgan) slotitem).getOrganQualityMap(slot).forEach((key,value) ->
							organScores.put(key,organScores.getOrDefault(key,0f)+(value*slot.getCount())));
				}
				else {
					//check vanilla organs
					if(VanillaOrgans.map.containsKey(slotitem)){
						VanillaOrgans.map.get(slotitem).forEach((key,value) ->
								organScores.put(key,organScores.getOrDefault(key,0f)+(value*slot.getCount())));
					}
				}
			}
		}
		if(!oldScores.equals(organScores))
		{
			if(ChestCavity.DEBUG_MODE) {
				System.out.println("Displaying organ scores:");
				organScores.forEach((key, value) ->
						System.out.print(key.toString() + ": " + value + " "));
				System.out.print("\n");
			}
			OrganUpdateCallback.EVENT.invoker().onOrganUpdate(player, oldScores, organScores);
			return true;
		}
		return false;
	}

	public float getOrganScore(Identifier id) {
		return organScores.getOrDefault(id, 0f);
	}
	
	public void setOrganScore(Identifier id, float value){
		organScores.put(id,value);
	}

	public float applyBoneDefense(float damage){
		float boneScore = organScores.getOrDefault(CCOrganScores.BONE,0f)
							+(organScores.getOrDefault(CCOrganScores.SPINE,0f)*3);
		return damage*(20/(1+boneScore));
	}
	
	public float applyIntestinesSaturation(float sat){
		return sat*organScores.getOrDefault(CCOrganScores.INTESTINE,0f)/4;
	}
	
	public int applyStomachHunger(int hunger){
		//sadly, in order to get saturation at all we must grant at least half a haunch of food, unless we embrace incompatability
		return Math.max((int)(hunger*organScores.getOrDefault(CCOrganScores.STOMACH,0f)),1);
	}
	
	//returns how much air we should attempt to lose
	public int applyLungCapacityInWater(){
		float airloss = 2f/Math.max(organScores.getOrDefault(CCOrganScores.LUNG,0f),.1f) + lungLeftover;
		lungLeftover = airloss % 1;
		return (int) airloss;
	}

	public int applySpleenMetabolism(int metatimer){
		spleenTimer++;
		if(spleenTimer >=2){
			metatimer += organScores.getOrDefault(CCOrganScores.SPLEEN,0f) - 1;
		}
		spleenTimer = 0;
		return metatimer;
	}




}
