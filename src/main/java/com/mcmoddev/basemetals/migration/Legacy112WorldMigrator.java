package com.mcmoddev.basemetals.migration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

import com.mcmoddev.basemetals.BaseMetals;
import com.mcmoddev.basemetals.content.BaseMetalAnvilBlock;
import com.mcmoddev.basemetals.content.CompatibilityDoubleSlabBlock;
import com.mcmoddev.basemetals.content.PlateBlock;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.BlockStateData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Converts Forge 1.12 numeric chunk sections to the first flattened palette
 * format before vanilla's data fixer sees them. Vanilla cannot infer the
 * pre-flattening numeric IDs of third-party blocks, even when their registry
 * names still exist in level.dat.
 */
public final class Legacy112WorldMigrator {
    private static final int LAST_SUPPORTED_LEGACY_DATA_VERSION = 1_343;
    private static final int PALETTED_PRE_PROTOCHUNK_DATA_VERSION = 1_456;
    private static final int REGION_HEADER_BYTES = 8_192;
    private static final int SECTOR_BYTES = 4_096;
    private static final String MARKER = "BASEMETALS_1_12_BLOCK_MIGRATION_COMPLETE.txt";
    private static final String BACKUP_DIRECTORY = "basemetals-1.12-region-backup";

    private Legacy112WorldMigrator() {}

    public static void migrateIfNeeded(Path worldRoot) throws IOException {
        Path root = worldRoot.toAbsolutePath().normalize();
        if (Files.isRegularFile(root.resolve(MARKER))) return;

        Path levelDat = root.resolve("level.dat");
        if (!Files.isRegularFile(levelDat)) return;
        CompoundTag level = NbtIo.readCompressed(levelDat.toFile());
        int dataVersion = level.getCompound("Data").getInt("DataVersion");
        Map<Integer, String> oldBlockIds = dataVersion <= LAST_SUPPORTED_LEGACY_DATA_VERSION
                ? oldBlockIds(level) : embeddedBlockIds();
        if (oldBlockIds.values().stream().noneMatch(id -> id.startsWith(BaseMetals.MOD_ID + ":"))) return;

        List<Path> regions = findRegionFiles(root);
        if (regions.isEmpty()) return;

        int convertedRegions = 0;
        int convertedChunks = 0;
        int externalStates = 0;
        for (Path region : regions) {
            RegionConversion result = convertRegion(root, region, oldBlockIds);
            if (result.convertedChunks() > 0) {
                convertedRegions++;
                convertedChunks += result.convertedChunks();
                externalStates += result.externalStates();
            }
        }

        if (convertedChunks == 0) {
            Files.writeString(root.resolve(MARKER),
                    "No Forge 1.12 numeric chunks required Base Metals migration." + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return;
        }

        String result = "Converted " + convertedChunks + " legacy chunks in " + convertedRegions
                + " region files before vanilla data fixing. Backups: " + BACKUP_DIRECTORY
                + ". External mod states reduced to their named default state: " + externalStates + ".";
        Files.writeString(root.resolve(MARKER), result + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        BaseMetals.LOGGER.info(result);
    }

    private static Map<Integer, String> oldBlockIds(CompoundTag level) {
        CompoundTag registries = level.getCompound("FML").getCompound("Registries");
        ListTag ids = registries.getCompound("minecraft:blocks").getList("ids", Tag.TAG_COMPOUND);
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Tag value : ids) {
            CompoundTag entry = (CompoundTag) value;
            result.put(entry.getInt("V"), entry.getString("K"));
        }
        return result;
    }

    private static Map<Integer, String> embeddedBlockIds() throws IOException {
        String resource = "/data/basemetals/migration/legacy_block_ids_1_12.json";
        try (InputStream input = Legacy112WorldMigrator.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing packaged migration map " + resource);
            JsonObject ids = JsonParser.parseReader(new java.io.InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("ids");
            Map<Integer, String> result = new LinkedHashMap<>();
            ids.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparingInt(Integer::parseInt)))
                    .forEach(entry -> result.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString()));
            return result;
        }
    }

    private static List<Path> findRegionFiles(Path root) throws IOException {
        List<Path> result = new ArrayList<>();
        try (var paths = Files.walk(root, 4)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca"))
                    .filter(path -> path.getParent() != null && path.getParent().getFileName().toString().equals("region"))
                    .filter(path -> !path.startsWith(root.resolve(BACKUP_DIRECTORY)))
                    .sorted()
                    .forEach(result::add);
        }
        return result;
    }

    private static void ensureBackupCapacity(Path root, List<Path> regions) throws IOException {
        long bytesRequired = 0;
        Path backupRoot = root.resolve(BACKUP_DIRECTORY);
        for (Path region : regions) {
            Path backup = backupRoot.resolve(root.relativize(region));
            if (!Files.exists(backup)) bytesRequired += Files.size(region);
        }
        FileStore store = Files.getFileStore(root);
        if (store.getUsableSpace() < bytesRequired + 64L * 1_024L * 1_024L) {
            throw new IOException("Insufficient space for the Base Metals 1.12 region backup: need "
                    + bytesRequired + " bytes plus a 64 MiB safety margin");
        }
    }

    private static RegionConversion convertRegion(Path root, Path region, Map<Integer, String> ids) throws IOException {
        byte[] source = Files.readAllBytes(region);
        if (source.length < REGION_HEADER_BYTES) throw new IOException("Invalid region header: " + region);
        ByteBuffer locations = ByteBuffer.wrap(source, 0, SECTOR_BYTES).order(ByteOrder.BIG_ENDIAN);
        byte[] timestamps = Arrays.copyOfRange(source, SECTOR_BYTES, REGION_HEADER_BYTES);
        List<RegionChunk> chunks = new ArrayList<>();
        int converted = 0;
        int externalStates = 0;

        for (int index = 0; index < 1_024; index++) {
            int location = locations.getInt(index * 4);
            if (location == 0) continue;
            int offset = location >>> 8;
            int position = offset * SECTOR_BYTES;
            if (position < REGION_HEADER_BYTES || position + 5 > source.length) {
                throw new IOException("Invalid chunk offset in " + region + " entry " + index);
            }
            int length = intAt(source, position);
            if (length < 1 || position + 4 + length > source.length) {
                throw new IOException("Invalid chunk length in " + region + " entry " + index);
            }
            int compression = source[position + 4] & 0xff;
            byte[] payload = Arrays.copyOfRange(source, position + 5, position + 4 + length);
            CompoundTag chunk = readChunk(payload, compression);
            ChunkConversion conversion = convertChunk(chunk, ids);
            byte[] record;
            if (conversion.converted()) {
                record = writeChunk(chunk);
                converted++;
                externalStates += conversion.externalStates();
            } else {
                record = Arrays.copyOfRange(source, position, position + 4 + length);
            }
            chunks.add(new RegionChunk(index, record));
        }

        if (converted == 0) return new RegionConversion(0, 0);
        ensureBackupCapacity(root, List.of(region));
        byte[] output = buildRegion(chunks, timestamps);
        Path backup = root.resolve(BACKUP_DIRECTORY).resolve(root.relativize(region));
        Files.createDirectories(backup.getParent());
        if (!Files.exists(backup)) Files.copy(region, backup, StandardCopyOption.COPY_ATTRIBUTES);

        Path temporary = region.resolveSibling(region.getFileName() + ".basemetals.tmp");
        Files.write(temporary, output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(temporary, region, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, region, StandardCopyOption.REPLACE_EXISTING);
        }
        return new RegionConversion(converted, externalStates);
    }

    private static ChunkConversion convertChunk(CompoundTag chunk, Map<Integer, String> ids) throws IOException {
        if (chunk.getInt("DataVersion") > LAST_SUPPORTED_LEGACY_DATA_VERSION) {
            return new ChunkConversion(false, 0);
        }
        CompoundTag level = chunk.getCompound("Level");
        ListTag sections = level.getList("Sections", Tag.TAG_COMPOUND);
        int externalStates = 0;
        boolean converted = false;
        for (Tag value : sections) {
            CompoundTag section = (CompoundTag) value;
            if (!section.contains("Blocks", Tag.TAG_BYTE_ARRAY)) continue;
            externalStates += convertSection(section, ids);
            converted = true;
        }
        if (converted) chunk.putInt("DataVersion", PALETTED_PRE_PROTOCHUNK_DATA_VERSION);
        return new ChunkConversion(converted, externalStates);
    }

    private static int convertSection(CompoundTag section, Map<Integer, String> ids) throws IOException {
        byte[] blocks = section.getByteArray("Blocks");
        byte[] metadata = section.getByteArray("Data");
        byte[] add = section.contains("Add", Tag.TAG_BYTE_ARRAY) ? section.getByteArray("Add") : null;
        ListTag palette = new ListTag();
        Map<CompoundTag, Integer> paletteIds = new LinkedHashMap<>();
        int[] values = new int[4_096];
        int externalStates = 0;

        for (int index = 0; index < values.length; index++) {
            int blockId = Byte.toUnsignedInt(blocks[index]);
            if (add != null) blockId |= nibble(add, index) << 8;
            int meta = nibble(metadata, index);
            String name = ids.get(blockId);
            if (name == null) {
                throw new IOException("Legacy chunk uses numeric block ID " + blockId
                        + " which is absent from the exact Base Metals 2.5.0 runtime map");
            }
            CompoundTag state = flattenedState(blockId, name, meta);
            if (!name.startsWith("minecraft:") && !name.startsWith(BaseMetals.MOD_ID + ":")) externalStates++;
            Integer paletteId = paletteIds.get(state);
            if (paletteId == null) {
                paletteId = palette.size();
                paletteIds.put(state, paletteId);
                palette.add(state);
            }
            values[index] = paletteId;
        }

        int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.size() - 1)));
        long[] packed = new long[(int) Math.ceil(values.length * (double) bits / 64.0D)];
        long mask = (1L << bits) - 1L;
        for (int index = 0; index < values.length; index++) {
            int bitIndex = index * bits;
            int longIndex = bitIndex >>> 6;
            int offset = bitIndex & 63;
            long value = values[index] & mask;
            packed[longIndex] |= value << offset;
            if (offset + bits > 64) packed[longIndex + 1] |= value >>> (64 - offset);
        }

        section.put("Palette", palette);
        section.put("BlockStates", new LongArrayTag(packed));
        section.remove("Blocks");
        section.remove("Data");
        section.remove("Add");
        return externalStates;
    }

    private static CompoundTag flattenedState(int oldId, String name, int metadata) {
        if (name.startsWith("minecraft:") && oldId >= 0 && oldId < 256) {
            Dynamic<?> dynamic = BlockStateData.getTag((oldId << 4) | metadata);
            Object value = dynamic.getValue();
            if (value instanceof CompoundTag state) return state.copy();
        }

        ResourceLocation id = ResourceLocation.tryParse(name);
        Block block = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        if (block != null && name.startsWith(BaseMetals.MOD_ID + ":")) {
            return NbtUtils.writeBlockState(stateForLegacyMetadata(block, metadata));
        }
        CompoundTag state = new CompoundTag();
        state.putString("Name", name);
        return state;
    }

    /** Maps Base Metals' persisted 1.12 metadata onto its 1.18 state schema. */
    public static BlockState stateForLegacyMetadata(Block block, int metadata) {
        BlockState state = block.defaultBlockState();
        if (block instanceof LiquidBlock) {
            return state.setValue(LiquidBlock.LEVEL, metadata & 15);
        }
        if (block instanceof PlateBlock) {
            return state.setValue(PlateBlock.FACING, Direction.from3DDataValue(metadata));
        }
        if (block instanceof CompatibilityDoubleSlabBlock) {
            return state.setValue(SlabBlock.TYPE, SlabType.DOUBLE).setValue(SlabBlock.WATERLOGGED, false);
        }
        if (block instanceof BaseMetalAnvilBlock) {
            return state.setValue(BaseMetalAnvilBlock.FACING, Direction.from2DDataValue(metadata & 3))
                    .setValue(BaseMetalAnvilBlock.DAMAGE, Math.min(2, (metadata & 15) >> 2));
        }
        if (block instanceof DoorBlock) {
            if ((metadata & 8) != 0) {
                return state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                        .setValue(DoorBlock.HINGE, (metadata & 1) != 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT)
                        .setValue(DoorBlock.POWERED, (metadata & 2) != 0);
            }
            return state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                    .setValue(DoorBlock.FACING, Direction.from2DDataValue(metadata & 3).getCounterClockWise())
                    .setValue(DoorBlock.OPEN, (metadata & 4) != 0);
        }
        if (block instanceof TrapDoorBlock) {
            Direction facing = switch (metadata & 3) {
                case 0 -> Direction.NORTH;
                case 1 -> Direction.SOUTH;
                case 2 -> Direction.WEST;
                default -> Direction.EAST;
            };
            return state.setValue(TrapDoorBlock.FACING, facing)
                    .setValue(TrapDoorBlock.OPEN, (metadata & 4) != 0)
                    .setValue(TrapDoorBlock.HALF, (metadata & 8) == 0 ? Half.BOTTOM : Half.TOP)
                    .setValue(TrapDoorBlock.WATERLOGGED, false);
        }
        if (block instanceof ButtonBlock) {
            Direction direction = legacyButtonDirection(metadata & 7);
            return state.setValue(ButtonBlock.FACE, attachFace(direction))
                    .setValue(ButtonBlock.FACING, horizontalAttachmentDirection(direction, metadata & 7))
                    .setValue(ButtonBlock.POWERED, (metadata & 8) != 0);
        }
        if (block instanceof LeverBlock) {
            int orientation = metadata & 7;
            Direction direction = legacyLeverDirection(orientation);
            return state.setValue(LeverBlock.FACE, attachFace(direction))
                    .setValue(LeverBlock.FACING, horizontalAttachmentDirection(direction, orientation))
                    .setValue(LeverBlock.POWERED, (metadata & 8) != 0);
        }
        if (block instanceof PressurePlateBlock) {
            return state.setValue(PressurePlateBlock.POWERED, metadata == 1);
        }
        if (block instanceof SlabBlock) {
            return state.setValue(SlabBlock.TYPE, (metadata & 8) == 0 ? SlabType.BOTTOM : SlabType.TOP)
                    .setValue(SlabBlock.WATERLOGGED, false);
        }
        if (block instanceof StairBlock) {
            return state.setValue(StairBlock.HALF, (metadata & 4) == 0 ? Half.BOTTOM : Half.TOP)
                    .setValue(StairBlock.FACING, Direction.from3DDataValue(5 - (metadata & 3)))
                    .setValue(StairBlock.SHAPE, StairsShape.STRAIGHT)
                    .setValue(StairBlock.WATERLOGGED, false);
        }
        return state;
    }

    /** Runtime fixture comparison, excluding neighbour-derived pane/wall state. */
    public static boolean matchesLegacyMetadata(BlockState actual, int metadata) {
        Block block = actual.getBlock();
        if (block instanceof IronBarsBlock || block instanceof WallBlock) return true;
        BlockState expected = stateForLegacyMetadata(block, metadata);
        if (block instanceof DoorBlock) {
            if ((metadata & 8) != 0) {
                return actual.getValue(DoorBlock.HALF) == expected.getValue(DoorBlock.HALF)
                        && actual.getValue(DoorBlock.HINGE) == expected.getValue(DoorBlock.HINGE)
                        && actual.getValue(DoorBlock.POWERED) == expected.getValue(DoorBlock.POWERED);
            }
            return actual.getValue(DoorBlock.HALF) == expected.getValue(DoorBlock.HALF)
                    && actual.getValue(DoorBlock.FACING) == expected.getValue(DoorBlock.FACING)
                    && actual.getValue(DoorBlock.OPEN) == expected.getValue(DoorBlock.OPEN);
        }
        if (block instanceof ButtonBlock || block instanceof PressurePlateBlock || block instanceof LiquidBlock) {
            // These states legitimately change while spawn chunks prepare.
            return true;
        }
        return actual.equals(expected);
    }

    private static Direction legacyButtonDirection(int orientation) {
        return switch (orientation) {
            case 0 -> Direction.DOWN;
            case 1 -> Direction.EAST;
            case 2 -> Direction.WEST;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.NORTH;
            default -> Direction.UP;
        };
    }

    private static Direction legacyLeverDirection(int orientation) {
        return switch (orientation) {
            case 0, 7 -> Direction.DOWN;
            case 1 -> Direction.EAST;
            case 2 -> Direction.WEST;
            case 3 -> Direction.SOUTH;
            case 4 -> Direction.NORTH;
            default -> Direction.UP;
        };
    }

    private static AttachFace attachFace(Direction direction) {
        return direction == Direction.DOWN ? AttachFace.CEILING
                : direction == Direction.UP ? AttachFace.FLOOR : AttachFace.WALL;
    }

    private static Direction horizontalAttachmentDirection(Direction direction, int orientation) {
        if (direction.getAxis().isHorizontal()) return direction;
        return switch (orientation) {
            case 0, 6 -> Direction.EAST;
            default -> Direction.SOUTH;
        };
    }

    private static int nibble(byte[] values, int index) {
        if (values.length == 0) return 0;
        int value = Byte.toUnsignedInt(values[index >>> 1]);
        return value >>> ((index & 1) * 4) & 15;
    }

    private static CompoundTag readChunk(byte[] payload, int compression) throws IOException {
        InputStream input = switch (compression) {
            case 1 -> new GZIPInputStream(new ByteArrayInputStream(payload));
            case 2 -> new InflaterInputStream(new ByteArrayInputStream(payload));
            case 3 -> new ByteArrayInputStream(payload);
            default -> throw new IOException("Unsupported region compression type " + compression);
        };
        try (DataInputStream data = new DataInputStream(input)) {
            return NbtIo.read(data);
        }
    }

    private static byte[] writeChunk(CompoundTag chunk) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (OutputStream deflater = new DeflaterOutputStream(compressed);
                DataOutputStream data = new DataOutputStream(deflater)) {
            NbtIo.write(chunk, data);
        }
        byte[] payload = compressed.toByteArray();
        ByteBuffer record = ByteBuffer.allocate(payload.length + 5).order(ByteOrder.BIG_ENDIAN);
        record.putInt(payload.length + 1);
        record.put((byte) 2);
        record.put(payload);
        return record.array();
    }

    private static byte[] buildRegion(List<RegionChunk> chunks, byte[] timestamps) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] header = new byte[REGION_HEADER_BYTES];
        System.arraycopy(timestamps, 0, header, SECTOR_BYTES, SECTOR_BYTES);
        int sector = 2;
        List<byte[]> records = new ArrayList<>();
        for (RegionChunk chunk : chunks) {
            int sectors = (chunk.record().length + SECTOR_BYTES - 1) / SECTOR_BYTES;
            if (sectors > 255) throw new IOException("Chunk exceeds the region format sector limit");
            putInt(header, chunk.index() * 4, sector << 8 | sectors);
            byte[] padded = Arrays.copyOf(chunk.record(), sectors * SECTOR_BYTES);
            records.add(padded);
            sector += sectors;
        }
        output.write(header);
        for (byte[] record : records) output.write(record);
        return output.toByteArray();
    }

    private static int intAt(byte[] bytes, int position) {
        return ByteBuffer.wrap(bytes, position, 4).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private static void putInt(byte[] bytes, int position, int value) {
        ByteBuffer.wrap(bytes, position, 4).order(ByteOrder.BIG_ENDIAN).putInt(value);
    }

    private record RegionChunk(int index, byte[] record) {}
    private record RegionConversion(int convertedChunks, int externalStates) {}
    private record ChunkConversion(boolean converted, int externalStates) {}
}
