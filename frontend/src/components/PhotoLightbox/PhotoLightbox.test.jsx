import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import PhotoLightbox from "./PhotoLightbox";

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key) => key }),
}));

const photos = ["/uploads/a.jpg", "/uploads/b.jpg"];

describe("PhotoLightbox", () => {
  it("shows the requested photo with a counter", () => {
    render(
      <PhotoLightbox photos={photos} index={0} onClose={jest.fn()} onNavigate={jest.fn()} />,
    );

    expect(screen.getByRole("img")).toHaveAttribute("src", "/uploads/a.jpg");
    expect(screen.getByText("1 / 2")).toBeInTheDocument();
  });

  it("closes on Escape and on overlay click, but not on image click", () => {
    const onClose = jest.fn();
    render(
      <PhotoLightbox photos={photos} index={0} onClose={onClose} onNavigate={jest.fn()} />,
    );

    fireEvent.click(screen.getByRole("img"));
    expect(onClose).not.toHaveBeenCalled();

    fireEvent.keyDown(document, { key: "Escape" });
    fireEvent.click(screen.getByTestId("photo-lightbox"));
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it("navigates with the side buttons and wraps around", () => {
    const onNavigate = jest.fn();
    render(
      <PhotoLightbox photos={photos} index={0} onClose={jest.fn()} onNavigate={onNavigate} />,
    );

    fireEvent.click(
      screen.getByLabelText("activities.details.photoViewer.next"),
    );
    expect(onNavigate).toHaveBeenCalledWith(1);

    fireEvent.click(
      screen.getByLabelText("activities.details.photoViewer.prev"),
    );
    expect(onNavigate).toHaveBeenCalledWith(1); // wraps from 0 back to the last photo
  });

  it("pages with the arrow keys", () => {
    const onNavigate = jest.fn();
    render(
      <PhotoLightbox photos={photos} index={1} onClose={jest.fn()} onNavigate={onNavigate} />,
    );

    fireEvent.keyDown(document, { key: "ArrowLeft" });
    expect(onNavigate).toHaveBeenCalledWith(0);
  });

  it("hides the arrows and counter for a single photo", () => {
    render(
      <PhotoLightbox
        photos={["/uploads/a.jpg"]}
        index={0}
        onClose={jest.fn()}
        onNavigate={jest.fn()}
      />,
    );

    expect(
      screen.queryByLabelText("activities.details.photoViewer.next"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("1 / 1")).not.toBeInTheDocument();
  });

  it("shows the owner's delete button and reports the index", () => {
    const onDelete = jest.fn();
    render(
      <PhotoLightbox
        photos={photos}
        index={1}
        onClose={jest.fn()}
        onNavigate={jest.fn()}
        onDelete={onDelete}
      />,
    );

    fireEvent.click(
      screen.getByLabelText("activities.details.photoViewer.delete"),
    );
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  it("hides the delete button without an onDelete handler", () => {
    render(
      <PhotoLightbox photos={photos} index={0} onClose={jest.fn()} onNavigate={jest.fn()} />,
    );

    expect(
      screen.queryByLabelText("activities.details.photoViewer.delete"),
    ).not.toBeInTheDocument();
  });
});
