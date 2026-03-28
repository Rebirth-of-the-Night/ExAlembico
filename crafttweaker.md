# CraftTweaker API

## `mods.exalembico.ExAlembico`

```ts
/**
 * Registers a block (i.e. all of its states) as a heat source for the alembic.
 * @param block     The block to register.
 * @param heatLevel The heat level emitted by the block.
 */
static registerHeatSourceBlock(block: IBlock, heatLevel: int)

/**
 * Registers a single block state as a heat source for the alembic.
 * @param state     The block state to register.
 * @param heatLevel The heat level emitted by the block state.
 */
static registerHeatSourceBlock(state: IBlockState, heatLevel: int)
```

## `mods.exalembico.Heater`

```ts
/**
 * Adds a recipe to the heater recipe registry with a contiguous range of heat levels.
 * @param fuel         The fuel item to be burned.
 * @param duration     The duration the fuel should burn for.
 * @param minHeatLevel The lowest heat level supported by the fuel.
 * @param maxHeatLevel The highest heat level supported by the fuel.
static addHeaterRangeRecipe(fuel: IIngredient, duration: int, minHeatLevel: int, maxHeatLevel: int)

/**
 * Adds a recipe to the heater recipe registry.
 * @param fuel       The fuel item to be burned.
 * @param duration   The duration the fuel should burn for.
 * @param heatLevels The heat levels supported by the fuel.
static addHeaterRecipe(fuel: IIngredient, duration: int, ...heatLevels: int[])

/**
 * Removes a recipe from the heater recipe registry by its fuel item.
 * @param fuel The fuel item burned in the recipe to remove.
 */
static removeHeaterRecipe(fuel: IIngredient)
```

## `mods.exalembico.Alembic`

```ts
/**
 * Starts a new alembic recipe builder.
 * @param duration  The time, in ticks, for the recipe to complete.
 */
static beginAlembicRecipe(duration: int): AlembicRecipeBuilder
```

## `mods.exalembico.AlembicRecipeBuilder`

```ts
/**
 * Adds valid heat levels for the recipe.
 * @param heatLevels The heat levels to add.
 * @return This same recipe builder, for chaining.
 */
setHeatLevels(...heatLevels: int[]): AlembicRecipeBuilder

/**
 * Adds a range of valid heat levels for the recipe.
 * @param minHeatLevel The lower bound on valid heat levels, inclusive.
 * @param maxHeatLevel The upper bound on valid heat levels, inclusive.
 * @return This same recipe builder, for chaining.
 */
setHeatLevelRange(minHeatLevel: int, maxHeatLevel: int): AlembicRecipeBuilder

/**
 * Adds an input item to the recipe.
 * @param ingredient    The item ingredient to add.
 * @param transformInto The output that the item transforms into. Defaults to nothing.
 * @return This same recipe builder, for chaining.
 */
setInputItem(ingredient: IIngredient, transformInto: IItemStack?): AlembicRecipeBuilder

/**
 * Adds an input fluid to the recipe.
 * @param fluid         The fluid ingredient to add.
 * @param transformInto The output that the fluid transforms into. Defaults to nothing.
 * @return This same recipe builder, for chaining.
 */
setInputFluid(fluid: ILiquidStack, transformInto: ILiquidStack?): AlembicRecipeBuilder

/**
 * Adds an output item to the recipe.
 * @param stack         The item output to add.
 * @param transformFrom The input that the item transforms from. Defaults to nothing.
 * @return This same recipe builder, for chaining.
 */
setOutputItem(stack: IItemStack, transformFrom: IIngredient?): AlembicRecipeBuilder

/**
 * Adds an output fluid to the recipe.
 * @param fluid The fluid output to add.
 * @return This same recipe builder, for chaining.
 */
setOutputFluid(fluid: ILiquidStack): AlembicRecipeBuilder

/**
 * Adds a bonus output item to the recipe.
 * @param stack The bonus item output to add.
 * @param odds  The odds, as a number between 0 and 1, of getting the output. Defaults to 1.
 * @return This same recipe builder, for chaining.
 */
setBonusOutputItem(stack: IItemStack, odds: float?): AlembicRecipeBuilder

/**
 * Builds the recipe and adds it to the alembic recipe registry.
 */
addToAlembic()

/**
 * Builds the recipe and adds it to the nether alembic recipe registry.
 */
addToNetherAlembic()
```
