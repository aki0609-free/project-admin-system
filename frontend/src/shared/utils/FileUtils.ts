export const fileToBase64 = (
  file: File,
): Promise<string> =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = () => {
      if (typeof reader.result === 'string') {
        resolve(reader.result)
        return
      }

      reject(new Error('base64変換に失敗しました。'))
    }

    reader.onerror = () => reject(reader.error)

    reader.readAsDataURL(file)
  })