import React from 'react';

/**
 * A reusable, styled image component for documentation screenshots.
 * Supports a `size` preset and an optional caption below the image.
 */
const Image = ({
  src,
  alt,
  size = ImageSize.medium,
  style,
  hideBorder,
  caption,
}: {
  src: string | {};
  alt: string;
  size?: ImageSize;
  style?: object;
  hideBorder?: boolean;
  caption?: string;
}): React.ReactElement => {
  const sizeStyles = {
    [ImageSize.small]: { maxWidth: '300px' },
    [ImageSize.medium]: { maxWidth: '600px' },
    [ImageSize.large]: { maxWidth: '100%' },
  };

  const defaultImageStyles = {
    ...sizeStyles[size],
    width: 'auto',
    height: 'auto',
    objectFit: 'contain' as const,
    border: hideBorder ? 'none' : '1px solid var(--ifm-color-emphasis-300)',
    borderRadius: '8px',
    margin: '0',
    padding: '0.5rem',
    display: 'block',
  };

  const combinedImageStyles = { ...defaultImageStyles, ...style };

  return (
    <figure
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '0.125rem',
        margin: '1.5rem 0',
        width: 'fit-content',
      }}
    >
      <img src={src as string} alt={alt} style={combinedImageStyles} />
      {caption && (
        <figcaption
          style={{ fontSize: '0.75rem', fontWeight: 'bold', textAlign: 'center' }}
        >
          {caption}
        </figcaption>
      )}
    </figure>
  );
};

export enum ImageSize {
  small = 'small',
  medium = 'medium',
  large = 'large',
}

export default Image;
