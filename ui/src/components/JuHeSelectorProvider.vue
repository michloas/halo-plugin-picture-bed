<script lang="ts" setup>
import {
  Dialog,
  IconCheckboxCircle,
  IconCheckboxFill,
  IconDeleteBin,
  IconEye,
  IconRefreshLine,
  IconUpload,
  VButton,
  VCard,
  VEmpty,
  VLoading,
  VPagination,
  VSpace,
  VTag,
} from '@halo-dev/components'
import { computed, ref, watch } from 'vue'
import { isImage } from '@/utils/image'
import type { AttachmentLike } from '@halo-dev/ui-shared'
import { matchMediaTypes } from '@/utils/media-type'
import LazyImage from '@/components/image/LazyImage.vue'
import { useQuery } from '@tanstack/vue-query'
import ImageDetailModal from '@/components/image/ImageDetailModal.vue'
import ImageUploadModal from '@/components/image/ImageUploadModal.vue'
import { pictureBedApisClient } from '@/api'
import AttachmentFileTypeIcon from '@/components/icon/AttachmentFileTypeIcon.vue'
import type { AlbumVO, ImageVO } from '@/api/generated'

const props = withDefaults(
  defineProps<{
    selected: AttachmentLike[]
    accepts?: string[]
    min?: number
    max?: number
    pictureBedKey: string
  }>(),
  {
    selected: () => [],
    accepts: () => ['*/*'],
    min: undefined,
    max: undefined,
    pictureBedKey: '',
  },
)

const emit = defineEmits<{
  (event: 'update:selected', attachments: AttachmentLike[]): void
  (event: 'change-provider', providerId: string): void
}>()

const selectedImages = ref<Set<ImageVO>>(new Set())
const deletedImageIds = ref<Set<string>>(new Set())
const selectedAlbum = ref<AlbumVO>()
const selectedImage = ref<ImageVO | undefined>()
const uploadVisible = ref(false)
const detailVisible = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(40)
const keyword = ref('')
const totalLabel = ref('')
const albumListIsLoading = ref(false)
const isLoading = ref(false)

const picturebedType = computed(() => props.pictureBedKey.split('_')[0])
const pictureBedId = computed(() => props.pictureBedKey.split('_')[1])

const { data: albumList } = useQuery({
  queryKey: [`albumList_${picturebedType.value}`, keyword],
  queryFn: async () => {
    albumListIsLoading.value = true
    const { data } = await pictureBedApisClient.pictureBed.albums({
      pictureBedId: pictureBedId.value,
      type: picturebedType.value,
      page: 1,
      size: 100,
      keyword: keyword.value,
    })
    const albums = [
      {
        id: '',
        name: '全部',
        description: '全部图片',
      },
      ...data,
    ]
    if (!selectedAlbum.value) {
      selectedAlbum.value = albums[0]
    }
    albumListIsLoading.value = false
    return albums
  },
})

const { data: imageList, refetch } = useQuery({
  queryKey: [`imageList_${picturebedType.value}`, selectedAlbum, page, size, keyword],
  queryFn: async () => {
    isLoading.value = true
    const { data } = await pictureBedApisClient.pictureBed.images({
      pictureBedId: pictureBedId.value,
      type: picturebedType.value,
      page: page.value,
      size: size.value,
      categories: selectedAlbum.value?.name === '全部' ? '' : selectedAlbum.value?.name,
    })

    totalLabel.value = `共 ${data.totalCount} 条`
    total.value = (data.size as number) * (data.totalPages as number)
    page.value = data.page as number
    size.value = data.size as number
    isLoading.value = false

    return (data.list as ImageVO[]).filter(
      (image) => !deletedImageIds.value.has(image.id as string),
    )
  },
  enabled: computed(() => selectedAlbum.value !== undefined),
})

const handleSelectAlbum = (album: AlbumVO) => {
  selectedAlbum.value = album
  selectedImages.value.clear()
  page.value = 1
}

const isChecked = (image: ImageVO) => selectedImages.value.has(image)

const isDisabled = (image: ImageVO) => {
  const isMatchMediaType = matchMediaTypes(image.mediaType || '*/*', props.accepts)
  return props.max !== undefined && props.max <= selectedImages.value.size && !isChecked(image)
    ? true
    : !isMatchMediaType
}

const deleteSelected = async () => {
  const selected = Array.from(selectedImages.value)
  Dialog.warning({
    title: '确认删除',
    description: `确定要删除选中的 ${selected.length} 张图片吗?此操作不可恢复。`,
    confirmText: '确定',
    cancelText: '取消',
    onConfirm: async () => {
      selected.forEach((image) => {
        pictureBedApisClient.pictureBed.deleteImage({
          pictureBedId: pictureBedId.value,
          type: picturebedType.value,
          imageId: image.id,
        })
        deletedImageIds.value.add(image.id as string)
      })
      selectedImages.value.clear()
      await refetch()
      emit('update:selected', [])
    },
  })
}

const handleSelect = (image: ImageVO) => {
  if (selectedImages.value.has(image)) {
    selectedImages.value.delete(image)
  } else {
    selectedImages.value.add(image)
  }
}

const handleSelectAll = () => {
  if (!imageList.value) return

  const allSelected = imageList.value.every((image) => selectedImages.value.has(image))

  if (allSelected) {
    // 如果全部已选中，则取消全选
    imageList.value.forEach((image) => {
      if (selectedImages.value.has(image)) {
        selectedImages.value.delete(image)
      }
    })
  } else {
    // 否则全选所有可选的图片
    imageList.value.forEach((image) => {
      if (!isDisabled(image)) {
        selectedImages.value.add(image)
      }
    })
  }
}

const isAllSelected = computed(() => {
  if (!imageList.value || imageList.value.length === 0) return false
  return imageList.value.every((image) => selectedImages.value.has(image))
})

const handleOpenDetail = (image: ImageVO) => {
  selectedImage.value = image
  detailVisible.value = true
}
// `watch` 不起作用可能是因为 `selectedImages` 是一个 `ref` 包装的 `Set`，`Set` 是引用类型，
// 直接修改 `Set` 内部元素不会触发响应式更新，使用 `watch` 的 `deep` 选项来深度监听
watch(
  selectedImages,
  () => {
    const images = Array.from(selectedImages.value).map((image) => ({
      spec: {
        displayName: image.name,
        mediaType: image.mediaType,
        size: image.size,
      },
      status: {
        permalink: image.url,
      },
    }))
    emit('update:selected', images as AttachmentLike[])
  },
  { deep: true },
)

watch(
  keyword,
  () => {
    selectedImages.value.clear()
    page.value = 1
  },
  { deep: true },
)

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / k ** i).toFixed(1)} ${sizes[i]}`
}

// 格式化日期时间（到秒）
const formatDateTime = (dateString: string): string => {
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}
</script>

<template>
  <div
    v-if="!keyword"
    class="topics mt-1 flex gap-x-2 gap-y-3 overflow-y-hidden overflow-x-hidden scroll-smooth pb-1"
  >
    <VLoading v-if="albumListIsLoading" />
    <div
      v-else
      v-for="(album, index) in albumList"
      :key="index"
      :class="{
        '!bg-primary !text-white shadow-md': album.id === selectedAlbum?.id,
      }"
      class="flex cursor-pointer items-center rounded bg-gray-100 px-3 py-2 text-sm text-gray-600 transition-all hover:bg-gray-200 hover:text-gray-900 hover:shadow-sm"
      @click="handleSelectAlbum(album)"
    >
      <div class="flex flex-1 items-center justify-between truncate">
        <span class="truncate text-sm">
          {{ album.name }}
        </span>
        <span
          v-if="album.count !== undefined"
          class="ml-2 shrink-0 rounded-full bg-gray-200 px-2 py-0.5 text-xs text-gray-600"
        >
          {{ album.count }}
        </span>
      </div>
    </div>
  </div>
  <div class="mt-3">
    <VSpace>
      <VButton @click="refetch">
        <template #icon>
          <IconRefreshLine class="h-full w-full" />
        </template>
        刷新
      </VButton>
      <VButton @click="uploadVisible = true">
        <template #icon>
          <IconUpload class="h-full w-full" />
        </template>
        上传
      </VButton>
      <VButton v-if="props.max !== 1" @click="handleSelectAll">
        <template #icon>
          <IconCheckboxCircle class="h-full w-full" />
        </template>
        {{ isAllSelected ? '取消全选' : '全选' }}
      </VButton>
      <VButton type="danger" v-if="selectedImages.size > 0" @click="deleteSelected">
        <template #icon>
          <IconDeleteBin class="h-full w-full" />
        </template>
        删除
      </VButton>
    </VSpace>
  </div>
  <VLoading v-if="isLoading" />
  <VEmpty
    v-else-if="imageList?.length === 0"
    message="当前分组没有附件，你可以尝试刷新或者上传附件"
    title="当前分组没有附件"
  >
    <template #actions>
      <VSpace>
        <VButton @click="refetch">刷新</VButton>
        <VButton type="secondary" @click="uploadVisible = true">
          <template #icon>
            <IconUpload class="h-full w-full" />
          </template>
          上传
        </VButton>
      </VSpace>
    </template>
  </VEmpty>

  <div
    v-else
    class="mt-2 grid grid-cols-3 gap-x-2 gap-y-3 sm:grid-cols-3 md:grid-cols-6 xl:grid-cols-8 2xl:grid-cols-10"
    role="list"
  >
    <VCard
      v-for="(image, index) in imageList"
      :key="index"
      :body-class="['!p-0']"
      :class="{
        'ring-1 ring-primary': isChecked(image),
        'pointer-events-none !cursor-not-allowed opacity-50': isDisabled(image),
      }"
      class="hover:shadow"
      @click.stop="handleSelect(image)"
    >
      <div class="group relative bg-white">
        <div
          class="aspect-h-8 aspect-w-10 block h-full w-full cursor-pointer overflow-hidden bg-gray-100"
        >
          <LazyImage
            v-if="isImage(image.mediaType)"
            :key="image.id"
            :alt="image.name"
            :src="image.url || ''"
            classes="pointer-events-none object-cover group-hover:opacity-75 transform-gpu"
          >
            <template #loading>
              <div class="flex h-full items-center justify-center object-cover">
                <span class="text-xs text-gray-400">加载中...</span>
              </div>
            </template>
            <template #error>
              <div class="flex h-full items-center justify-center object-cover">
                <span class="text-xs text-red-400">加载异常</span>
              </div>
            </template>
          </LazyImage>
          <AttachmentFileTypeIcon v-else :file-name="image.name" />
        </div>
        <div class="px-2 py-1.5">
          <p
            class="pointer-events-none block truncate text-center text-xs font-medium text-gray-700"
          >
            {{ image.name }}
          </p>
          <div
            v-if="image.categories && image.categories.length > 0"
            class="mt-1 flex flex-wrap justify-center gap-1"
          >
            <VTag v-for="(category, idx) in image.categories" :key="idx" type="danger" size="sm">
              {{ category }}
            </VTag>
          </div>
          <div class="mt-1 flex items-center justify-center gap-2 text-xs text-gray-500">
            <span v-if="image.createTime" class="flex items-center gap-0.5">
              <IconCalendarLine class="h-3 w-3" />
              {{ formatDateTime(image.createTime) }}
            </span>
            <span v-if="image.size" class="flex items-center gap-0.5">
              <IconDatabase2Line class="h-3 w-3" />
              {{ formatFileSize(image.size) }}
            </span>
          </div>
        </div>

        <div
          :class="{ '!flex': selectedImages.has(image) }"
          class="absolute left-0 top-0 hidden h-1/3 w-full justify-end bg-gradient-to-b from-gray-300 to-transparent ease-in-out group-hover:flex"
        >
          <IconEye
            class="mr-1 mt-1 hidden h-6 w-6 cursor-pointer text-white transition-all hover:text-primary group-hover:block"
            @click.stop="handleOpenDetail(image)"
          />
          <IconCheckboxFill
            :class="{ '!text-primary': selectedImages.has(image) }"
            class="mr-1 mt-1 h-6 w-6 cursor-pointer text-white transition-all hover:text-primary"
          />
        </div>
      </div>
    </VCard>
  </div>

  <div class="mt-4">
    <VPagination
      v-model:page="page"
      v-model:size="size"
      page-label="页"
      size-label="条 / 页"
      :total-label="totalLabel"
      :total="total"
      :size-options="[40, 80, 120]"
    />
  </div>

  <ImageDetailModal
    v-model:visible="detailVisible"
    :mount-to-body="true"
    :imageSelected="selectedImage"
    @close="detailVisible = false"
  >
    <template #actions>
      <span
        v-if="selectedImage && selectedImages.has(selectedImage)"
        @click="handleSelect(selectedImage)"
      >
        <IconCheckboxFill />
      </span>
      <span v-else @click="handleSelect(selectedImage as ImageVO)">
        <IconCheckboxCircle />
      </span>
    </template>
  </ImageDetailModal>

  <ImageUploadModal
    :visible="uploadVisible"
    :picBedType="picturebedType"
    :picBedId="pictureBedId"
    :categories="selectedAlbum?.name || ''"
    :key="selectedAlbum?.name"
    @close="
      uploadVisible = false;
      refetch();
    "
  />
</template>
