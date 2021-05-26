export default function readBinaryFileAsBase64String(file: File): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === "string") {
        const base64String = btoa(reader.result);
        resolve(base64String);
      } else {
        reject(new Error("Invalid or empty reader result"));
      }
    };
    reader.onerror = () => reject(reader.error);
    reader.readAsBinaryString(file);
  });
}
