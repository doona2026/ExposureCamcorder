package io.github.exposure_camcorder.neoforge;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.MapCodec;
import io.github.exposure_camcorder.ExposureCamcorder;
import io.github.exposure_camcorder.Register;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RegisterImpl {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ExposureCamcorder.ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ExposureCamcorder.ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExposureCamcorder.ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, ExposureCamcorder.ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, ExposureCamcorder.ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ExposureCamcorder.ID);
    public static final DeferredRegister<CriterionTrigger<?>> CRITERION_TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<ItemSubPredicate.Type<?>> ITEM_SUB_PREDICATES =
            DeferredRegister.create(Registries.ITEM_SUB_PREDICATE_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<MapCodec<? extends EntitySubPredicate>> ENTITY_SUB_PREDICATES =
            DeferredRegister.create(Registries.ENTITY_SUB_PREDICATE_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<Feature<?>> WORLD_GEN_FEATURES =
            DeferredRegister.create(Registries.FEATURE, ExposureCamcorder.ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ExposureCamcorder.ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ExposureCamcorder.ID);

    public static <T extends Block> Supplier<T> block(String id, Supplier<T> supplier) {
        return BLOCKS.register(id, supplier);
    }

    public static <T extends BlockEntityType<E>, E extends BlockEntity> Supplier<T> blockEntityType(String id,
                                                                                                    Supplier<T> supplier) {
        return BLOCK_ENTITY_TYPES.register(id, supplier);
    }

    public static <T extends BlockEntity> BlockEntityType<T> newBlockEntityType(Register.BlockEntitySupplier<T> supplier,
                                                                                Block... validBlocks) {
        try {
            Class<?> supplierClass =
                    Class.forName("net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
            Object blockEntitySupplier = Proxy.newProxyInstance(
                    RegisterImpl.class.getClassLoader(),
                    new Class<?>[]{supplierClass},
                    (proxy, method, args) -> supplier.create(
                            (net.minecraft.core.BlockPos) args[0],
                            (net.minecraft.world.level.block.state.BlockState) args[1]));
            Method ofMethod = BlockEntityType.Builder.class.getMethod("of", supplierClass, Block[].class);
            Object builder = ofMethod.invoke(null, blockEntitySupplier, validBlocks);
            Method buildMethod = BlockEntityType.Builder.class.getMethod("build", com.mojang.datafixers.types.Type.class);
            @SuppressWarnings("unchecked")
            BlockEntityType<T> blockEntityType = (BlockEntityType<T>) buildMethod.invoke(builder, new Object[]{null});
            return blockEntityType;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create NeoForge block entity type", e);
        }
    }

    public static <T extends Item> Supplier<T> item(String id, Supplier<T> supplier) {
        return ITEMS.register(id, supplier);
    }

    public static <T extends CreativeModeTab> Supplier<T> creativeTab(String id, Supplier<T> supplier) {
        return CREATIVE_MODE_TABS.register(id, supplier);
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                                        MobCategory category, float width, float height,
                                                                        int clientTrackingRange, boolean velocityUpdates,
                                                                        int updateInterval) {
        return ENTITY_TYPES.register(id, () -> EntityType.Builder.of(factory, category)
                .sized(width, height)
                .clientTrackingRange(clientTrackingRange)
                .updateInterval(updateInterval)
                .build(id));
    }

    public static <T extends Entity> Supplier<EntityType<T>> entityType(String id, EntityType.EntityFactory<T> factory,
                                                                        MobCategory category, boolean receiveVelocityUpdates,
                                                                        Consumer<EntityType.Builder<T>> typeBuilder) {
        return ENTITY_TYPES.register(id, () -> {
            EntityType.Builder<T> builder = EntityType.Builder.of(factory, category);
            typeBuilder.accept(builder);
            return builder.build(id);
        });
    }

    public static <T extends SoundEvent> Supplier<T> soundEvent(String id, Supplier<T> supplier) {
        return SOUND_EVENTS.register(id, supplier);
    }

    public static <T extends MenuType<E>, E extends AbstractContainerMenu> Supplier<MenuType<E>> menuType(String id,
                                                                                                           Register.MenuTypeSupplier<E> supplier) {
        return MENU_TYPES.register(id, () -> IMenuTypeExtension.create(supplier::create));
    }

    public static Supplier<RecipeType<?>> recipeType(String id, Supplier<RecipeType<?>> supplier) {
        return RECIPE_TYPES.register(id, supplier);
    }

    public static Supplier<RecipeSerializer<?>> recipeSerializer(String id, Supplier<RecipeSerializer<?>> supplier) {
        return RECIPE_SERIALIZERS.register(id, supplier);
    }

    public static <T extends CriterionTrigger<?>> Supplier<T> criterionTrigger(String id, Supplier<T> supplier) {
        return CRITERION_TRIGGERS.register(id, supplier);
    }

    public static <T extends ItemSubPredicate.Type<?>> Supplier<T> itemSubPredicate(String id, Supplier<T> supplier) {
        return ITEM_SUB_PREDICATES.register(id, supplier);
    }

    public static <T extends MapCodec<EntitySubPredicate>> Supplier<T> entitySubPredicate(String id, Supplier<T> supplier) {
        return ENTITY_SUB_PREDICATES.register(id, supplier);
    }

    public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>>
    Supplier<ArgumentTypeInfo<A, T>> commandArgumentType(String id, Class<A> infoClass, I argumentTypeInfo) {
        return COMMAND_ARGUMENT_TYPES.register(id, () -> argumentTypeInfo);
    }

    public static <T extends FeatureConfiguration> Supplier<Feature<?>> worldGenFeature(String id,
                                                                                        Supplier<Feature<T>> supplier) {
        return WORLD_GEN_FEATURES.register(id, supplier);
    }

    public static <T> DataComponentType<T> dataComponentType(String id,
                                                             Consumer<DataComponentType.Builder<T>> builderConsumer) {
        DataComponentType.Builder<T> builder = DataComponentType.builder();
        builderConsumer.accept(builder);
        DataComponentType<T> componentType = builder.build();
        DATA_COMPONENT_TYPES.register(id, () -> componentType);
        return componentType;
    }

    public static <T extends ParticleType<? extends ParticleOptions>> Supplier<ParticleType<?>> particleType(String id,
                                                                                                              Supplier<T> supplier) {
        return PARTICLE_TYPES.register(id, supplier);
    }
}
